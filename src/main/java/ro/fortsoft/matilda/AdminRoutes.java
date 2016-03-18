/*
 * Copyright (C) 2016 the original author or authors.
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
import ro.fortsoft.matilda.domain.CustomerDto;
import ro.fortsoft.matilda.domain.Document;
import ro.fortsoft.matilda.domain.User;
import ro.fortsoft.matilda.util.NetUtils;
import ro.fortsoft.matilda.util.WhiteList;
import ro.pippo.core.FileItem;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.RedirectHandler;
import ro.pippo.core.route.CSRFHandler;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteGroup;
import ro.pippo.core.route.Router;
import ro.pippo.core.util.PathRegexBuilder;
import ro.pippo.core.util.StringUtils;

import java.io.File;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Decebal Suiu
 */
public class AdminRoutes extends RouteGroup {

    private static final Logger log = LoggerFactory.getLogger(AdminRoutes.class);

    private final PippoApplication application;

    public AdminRoutes(PippoApplication application) {
        super("/admin");

        this.application = application;

        GET("/", new RedirectHandler("/admin/storage"));

        addRoutes();
    }

    private void addRoutes() {
        addBeforeFilters();

        addSecurityRoutes();
        addCustomersRoutes();
        addCompanyRoutes();
        addUserRoutes();
        addUploadRoutes();
        addStorageRoutes();
    }

    private void addBeforeFilters() {
        // admin IP white list
        ALL("/.*", (routeContext) -> {
            WhiteList whiteList = new WhiteList(getPippoSettings().getStrings("admin.whitelist"));
            String remoteIpAddress = NetUtils.getRemoteHost(routeContext.getRequest());

            if (!whiteList.isWhiteIp(remoteIpAddress)) {
                log.warn("Forbidden access to admin section for '{}'", remoteIpAddress);
                routeContext.getResponse().forbidden().send(getMessages().get("admin.forbidden", routeContext));
            } else {
                routeContext.next();
            }
        });

        // make "user" available for all templates
        GET(secureAdminPaths(), (routeContext) -> {
            routeContext.setLocal(PippoApplication.USER, getUser(routeContext));

            routeContext.next();
        }).named("customerCompanyFilter");

        // authentication user filter
        ALL(secureAdminPaths(), (routeContext) -> {
            if (getUser(routeContext) == null) {
                routeContext.redirect("/admin/login");
            } else {
                routeContext.next();
            }
        }).named("secureAdminFilter");

        /*
         * Register a CSRF token generator and validator.
         * This creates a session for all matching requests.
         */
        ALL(csrfAdminPaths(), new CSRFHandler()).named("CSRF handler");
    }

    private void addSecurityRoutes() {
        GET("/login", (routeContext) -> routeContext.render("admin/login")).named("adminLogin");

        POST("/login", (routeContext) -> {
            String username = routeContext.getParameter("username").toString();
            String password = routeContext.getParameter("password").toString();

            User user = authenticateUser(username, password);

            if (user != null) {
                routeContext.resetSession();
                log.debug("Authenticated user '{}'", user);
                routeContext.setSession(PippoApplication.USER, user);
                routeContext.redirect("/admin");
            } else {
                routeContext.flashError(getMessages().get("admin.login.invalid", routeContext));
                routeContext.redirect("/admin/login");
            }
        });

        GET("/logout", (routeContext) -> {
            routeContext.resetSession();
            routeContext.redirect("/admin/login");
        }).named("adminLogout");
    }

    private void addCustomersRoutes() {
        GET("/customers", (routeContext) -> {
//            routeContext.setLocal("companyService", companyService); // bug in pebble
            // workaround bug
            List<CustomerDto> customers = new ArrayList<>();
            for (Customer customer : application.getCustomerService().findAll()) {
                Company company = application.getCompanyService().findById(customer.getCompanyId());
                customers.add(new CustomerDto(customer, company));
            }
            routeContext.setLocal("customers", customers);
            routeContext.render("admin/customers");
        }).named("getCustomers");

        GET("/customer/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            Customer customer;
            if (id > 0) {
                customer = application.getCustomerService().findById(id);
            } else {
                customer = new Customer();
            }
            routeContext.setLocal(PippoApplication.CUSTOMER, customer);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            Router router = application.getRouter();
            routeContext.setLocal("saveUrl", router.uriFor("postCustomer", parameters));
            routeContext.setLocal("backUrl", router.uriFor("getCustomers", new HashMap<>()));
            routeContext.setLocal("companies", application.getCompanyService().findAll());

            routeContext.render("admin/customer");
        });

        POST("/customer", (routeContext) -> {
            Customer entity = routeContext.createEntityFromParameters(Customer.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            application.getCustomerService().save(entity);

            routeContext.getResponse().header("X-IC-Transition", "none");
//            routeContext.setLocal("customers", customerService.findAll());
            // workaround bug
            List<CustomerDto> customers = new ArrayList<>();
            for (Customer customer : application.getCustomerService().findAll()) {
                Company company = application.getCompanyService().findById(customer.getCompanyId());
                customers.add(new CustomerDto(customer, company));
            }
            routeContext.setLocal("customers", customers);

            routeContext.redirect("getCustomers", new HashMap<>());
        }).named("postCustomer");

        DELETE("/customer/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            application.getCustomerService().deleteById(id);

            routeContext.getResponse().header("X-IC-Remove", "true").commit();
        });
    }

    private void addCompanyRoutes() {
        GET("/companies", (routeContext) -> {
            routeContext.setLocal("companies", application.getCompanyService().findAll());
            routeContext.render("admin/companies");
        }).named("getCompanies");

        GET("/company/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            Company company = (id > 0) ? application.getCompanyService().findById(id) : new Company();
            routeContext.setLocal(PippoApplication.COMPANY, company);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            Router router = application.getRouter();
            routeContext.setLocal("saveUrl", router.uriFor("postCompany", parameters));
            routeContext.setLocal("backUrl", router.uriFor("getCompanies", new HashMap<>()));

            routeContext.render("admin/company");
        });

        POST("/company", (routeContext) -> {
            Company entity = routeContext.createEntityFromParameters(Company.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            application.getCompanyService().save(entity);

            routeContext.redirect("getCompanies", new HashMap<>());
        }).named("postCompany");
    }

    private void addUserRoutes() {
        GET("/user/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            User user = (id > 0) ? application.getUserService().findById(id) : new User();
            routeContext.setLocal(PippoApplication.USER, user);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            routeContext.setLocal("saveUrl", application.getRouter().uriFor("postUser", parameters));
            routeContext.setLocal("backUrl", "/admin");

            routeContext.render("admin/user");
        });

        POST("/user", (routeContext) -> {
            User entity = routeContext.createEntityFromParameters(User.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            // for now, allow only edit user
            if ((entity.getId() != null) && (entity.getId() == 1)) {
                application.getUserService().save(entity);
            }

            routeContext.redirect("/admin");
        }).named("postUser");
    }

    private void addUploadRoutes() {
        GET("/upload", (routeContext) -> {
            Long companyId = application.getCompanyId(routeContext);
            YearMonth date = application.getDate(routeContext);

            // for filter
            routeContext.setLocal("companies", application.getCompanyService().findAll());
            routeContext.setLocal(PippoApplication.COMPANY_ID, companyId);
            routeContext.setLocal(PippoApplication.DATE, date.format(PippoApplication.DATE_TIME_FORMATTER));

            if (companyId == null) {
                routeContext.getResponse().getFlash().error(getMessages().get("admin.company.required", routeContext));
            } else {
                Document example = new Document()
                    .setCompanyId(companyId)
                    .setYear(date.getYear())
                    .setMonth(date.getMonthValue())
                    .setType(Document.OUT_TYPE);

                List<Document> documents = application.getDocumentService().findByExample(example);

                routeContext.setLocal("documents", documents);
            }

            routeContext.render("admin/upload");
        }).named("adminUpload");

        POST("/upload", (routeContext) -> {
            FileItem file = routeContext.getRequest().getFile("files");

            Long companyId = application.getCompanyId(routeContext);
            YearMonth date = application.getDate(routeContext);

            application.uploadFile(file, companyId, date, Document.OUT_TYPE);

            // send response
            // we must send a json http://plugins.krajee.com/file-input#ajax-uploads
            routeContext.json().send("{}");
        });
    }

    private void addStorageRoutes() {
        GET("/storage", (routeContext) -> {
            Long companyId = application.getCompanyId(routeContext);
            YearMonth date = application.getDate(routeContext);

            List<Document> documents = null;
            if (companyId != null) {
                Document example = new Document()
                    .setCompanyId(companyId)
                    .setYear(date.getYear())
                    .setMonth(date.getMonthValue())
                    .setType(Document.IN_TYPE);

                documents = application.getDocumentService().findByExample(example);
            }

            routeContext.setLocal("documents", documents);

            // for filter
            routeContext.setLocal("companies", application.getCompanyService().findAll());
            routeContext.setLocal(PippoApplication.COMPANY_ID, companyId);
            routeContext.setLocal(PippoApplication.DATE, date.format(PippoApplication.DATE_TIME_FORMATTER));

            if (companyId == null) {
                routeContext.getResponse().getFlash().error(getMessages().get("admin.company.required", routeContext));
            }

            routeContext.render("admin/storage");
        }).named("storage");

        GET("/download", (routeContext) -> {
            Long companyId = application.getCompanyId(routeContext);
            YearMonth date = application.getDate(routeContext);

            File zip = application.createZip(companyId, date, Document.IN_TYPE);

            // send the zip file
            routeContext.getResponse().file(zip);
        }).named("adminDownload");
    }

    private PippoSettings getPippoSettings() {
        return application.getPippoSettings();
    }

    private Messages getMessages() {
        return application.getMessages();
    }

    private String secureAdminPaths() {
        return new PathRegexBuilder()
            .includes(
                "/admin"
            )
            .excludes(
                "/admin/login",
                "/webjars",
                "/public"
            )
            .build();
    }

    private String csrfAdminPaths() {
        return new PathRegexBuilder()
            .includes(
                "/admin/customer.*",
                "/admin/company.*"
            )
            .build();
    }

    private User getUser(RouteContext routeContext) {
        return routeContext.getSession(PippoApplication.USER);
    }

    private User authenticateUser(String username, String password) {
        if (StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password)) {
            return null;
        }

        User user = application.getUserService().findByUsername(username);
        if (user == null) {
            log.debug("Cannot found user with username '{}'", username);
        } else if (!password.equals(user.getPassword())) {
            log.debug("Password mismatch");
            user = null;
        }

        return user;
    }

}
