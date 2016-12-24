/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.fortsoft.matilda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.matilda.domain.Company;
import ro.fortsoft.matilda.domain.Customer;
import ro.fortsoft.matilda.domain.Document;
import ro.fortsoft.matilda.domain.FileSystemStorage;
import ro.fortsoft.matilda.domain.Storage;
import ro.fortsoft.matilda.service.CompanyService;
import ro.fortsoft.matilda.service.CustomerService;
import ro.fortsoft.matilda.service.DocumentService;
import ro.fortsoft.matilda.service.ServiceFactory;
import ro.fortsoft.matilda.service.UserService;
import ro.fortsoft.matilda.util.DbUtils;
import ro.fortsoft.matilda.util.ZipUtils;
import ro.fortsoft.matilda.web.AdminRoutes;
import ro.fortsoft.matilda.web.ExtendedPebbleTemplateEngine;
import ro.fortsoft.matilda.web.Routes;
import ro.pippo.core.Application;
import ro.pippo.core.FileItem;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.RedirectHandler;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.util.PathRegexBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple Pippo application.
 *
 * @see MatildaLauncher#main(String[])
 */
public class MatildaApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(MatildaApplication.class);

    public static final String DATE = "date";
    public static final String USER = "user";
    public static final String CUSTOMER = "customer";
    public static final String COMPANY = "company";
    public static final String COMPANY_ID = "companyId";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-yyyy");

    private static final String UPLOAD_LOCATION = "uploads";

    private CustomerService customerService;
    private CompanyService companyService;
    private UserService userService;
    private DocumentService documentService;
    private Storage storage;

    public UserService getUserService() {
        return userService;
    }

    public CompanyService getCompanyService() {
        return companyService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    @Override
    protected void onInit() {
        setTemplateEngine(new ExtendedPebbleTemplateEngine());

        createServices();

        // set upload location
        setUploadLocation(UPLOAD_LOCATION);

        // add routes for static content
        addPublicResourceRoute();
        addWebjarsResourceRoute();

        getRouter().ignorePaths("/favicon.ico");

        GET("/", new RedirectHandler("/upload"));

        // BEFORE FILTERS
        addBeforeFilters();

        // ROUTES
        addRouteGroup(new Routes(this));
        addRouteGroup(new AdminRoutes(this));

        // AFTER FILTERS
        addAfterFilters();
    }

    private void addBeforeFilters() {
        // authentication customer filter
        ALL(securePaths(), routeContext -> {
            if (routeContext.getSession(CUSTOMER) == null) {
                routeContext.redirect("/login");
            } else {
                routeContext.next();
            }
        }).named("secureCustomerFilter");

        // make "customer" and "company" available for all templates
        GET(securePaths(), routeContext -> {
            routeContext.setLocal(CUSTOMER, getCustomer(routeContext));
            routeContext.setLocal(COMPANY, getCompany(routeContext));

            routeContext.next();
        }).named("customerCompanyFilter");

        // debtor filter
        GET(securePaths(), routeContext -> {
            Customer customer = getCustomer(routeContext);
            if (customer.isDebtor()) {
                routeContext.render("debtor");
            } else {
                routeContext.next();
            }
        }).named("debtorFilter");
    }

    public File createZip(long companyId, YearMonth date, String documentType) {
        String zipName = new StringBuilder()
            .append(companyService.findById(companyId).getFiscalCode())
            .append('-')
            .append(date.getMonthValue())
            .append('-')
            .append(date.getYear())
            .append(".zip")
            .toString();

        log.debug("Zip file '{}' for company '{}' @ '{}'", zipName, companyId, date.format(DATE_TIME_FORMATTER));

        Document example = new Document()
            .setCompanyId(companyId)
            .setYear(date.getYear())
            .setMonth(date.getMonthValue());

        if (documentType != null) {
            example.setType(documentType);
        }

        List<Document> documents = documentService.findByExample(example);

        Map<String, InputStream> inputStreams = new HashMap<>();
        for (Document document : documents) {
            inputStreams.put(document.getName(), storage.getStream(document));
        }

        try {
            return ZipUtils.zip(zipName, inputStreams);
        } catch (Exception e) {
            throw new PippoRuntimeException(e);
        }
    }

    public void uploadFile(FileItem file, long companyId, YearMonth date, String type) {
        log.debug("Upload file '{}' for company '{}' @ '{}'", file, companyId, date.format(DATE_TIME_FORMATTER));

        Document document = new Document()
            .setCompanyId(companyId)
            .setYear(date.getYear())
            .setMonth(date.getMonthValue())
            .setUploadedDate(new Date())
            .setType(type)
            .setName(file.getSubmittedFileName());

        InputStream inputStream;
        try{
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new PippoRuntimeException(e);
        }

        long size = storage.store(inputStream, document);

        if (size > 0) {
            document.setSize(size);
            documentService.save(document);
        }
    }

    public Company getCompany(RouteContext routeContext) {
        Long companyId = routeContext.getParameter(COMPANY_ID).toLong();

        Company company;
        if ((companyId != null) && (companyId > 0)) {
            company = companyService.findById(companyId);
            if (!Objects.equals(company, routeContext.getSession(COMPANY))) {
                routeContext.setSession(COMPANY, company);
                routeContext.setLocal(COMPANY, company);
            }
        } else {
            company = routeContext.getSession(COMPANY);
            routeContext.setLocal(COMPANY, company);
        }

        if (company == null) {
            Customer customer = getCustomer(routeContext);
            if (customer != null) {
                company = companyService.findById(customer.getCompanyId());
                routeContext.setSession(COMPANY, company);
            }
        }

        return company;
    }

    public Long getCompanyId(RouteContext routeContext) {
        Company company = getCompany(routeContext);

        return company != null ? company.getId() : null;
    }

    public Customer getCustomer(RouteContext routeContext) {
        return routeContext.getSession(CUSTOMER);
    }

    public YearMonth getDate(RouteContext routeContext) {
        String dateParameter = routeContext.getParameter(DATE).toString();

        YearMonth date;
        if (dateParameter != null) {
            date = YearMonth.parse(dateParameter, DATE_TIME_FORMATTER);
            if (!Objects.equals(date, routeContext.getSession(DATE))) {
                routeContext.setSession(DATE, date);
            }
        } else {
            date = routeContext.getSession(DATE);
        }

        if (date == null) {
            date = YearMonth.now().minusMonths(1);
            routeContext.setSession(DATE, date);
        }

        return date;
    }

    private void createServices() {
        ServiceFactory serviceFactory = new ServiceFactory();

        customerService = serviceFactory.createCustomerService();
        companyService = serviceFactory.createCompanyService();
        userService = serviceFactory.createUserService();
        documentService = serviceFactory.createDocumentService();

        storage = new FileSystemStorage(UPLOAD_LOCATION);
    }

    private void addAfterFilters() {
        ALL("/.*", routeContext -> DbUtils.closeDb()).runAsFinally();
    }

    private String securePaths() {
        return new PathRegexBuilder()
            .excludes(
                "/login",
                "/admin",
                "/webjars",
                "/public"
            )
            .build();
    }

}
