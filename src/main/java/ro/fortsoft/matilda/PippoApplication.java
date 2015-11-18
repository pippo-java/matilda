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
import ro.fortsoft.matilda.domain.CustomerDto;
import ro.fortsoft.matilda.domain.Document;
import ro.fortsoft.matilda.domain.User;
import ro.fortsoft.matilda.service.CompanyService;
import ro.fortsoft.matilda.service.CustomerService;
import ro.fortsoft.matilda.service.DocumentService;
import ro.fortsoft.matilda.service.ServiceFactory;
import ro.fortsoft.matilda.service.UserService;
import ro.fortsoft.matilda.util.DbUtils;
import ro.fortsoft.matilda.util.NetUtils;
import ro.fortsoft.matilda.util.RecaptchaUtils;
import ro.fortsoft.matilda.util.WhiteList;
import ro.fortsoft.matilda.util.ZipUtils;
import ro.pippo.core.Application;
import ro.pippo.core.FileItem;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.RedirectHandler;
import ro.pippo.core.Request;
import ro.pippo.core.route.CSRFHandler;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.util.PathRegexBuilder;
import ro.pippo.core.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple Pippo application.
 *
 * @see ro.fortsoft.matilda.PippoLauncher#main(String[])
 */
public class PippoApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(PippoApplication.class);

    private static final String UPLOAD_LOCATION = "uploads";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-yyyy");

    private static final String DATE = "date";
    private static final String CUSTOMER = "customer";
    private static final String COMPANY = "company";
    private static final String COMPANY_ID = "companyId";
    private static final String USER = "user";

    private CustomerService customerService;
    private CompanyService companyService;
    private UserService userService;
    private DocumentService documentService;
    private Storage storage;

    private Map<String, Integer> failedLoginByHost = new ConcurrentHashMap<>();

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

        // authentication customer filter
        ALL(securePaths(), (routeContext) -> {
            if (routeContext.getSession(CUSTOMER) == null) {
                routeContext.redirect("/login");
            } else {
                routeContext.next();
            }
        }).named("secureCustomerFilter");

        // admin IP white list
        ALL("/admin/.*", (routeContext) -> {
            WhiteList whiteList = new WhiteList(getPippoSettings().getStrings("admin.whitelist"));
            String remoteIpAddress = NetUtils.getRemoteHost(routeContext.getRequest());

            if (!whiteList.isWhiteIp(remoteIpAddress)) {
                log.warn("Forbidden access to admin section for '{}'", remoteIpAddress);
                routeContext.getResponse().forbidden().send(getMessages().get("admin.forbidden", routeContext));
            } else {
                routeContext.next();
            }
        });

        // authentication user filter
        ALL(secureAdminPaths(), (routeContext) -> {
            if (getUser(routeContext) == null) {
                routeContext.redirect("/admin/login");
            } else {
                routeContext.next();
            }
        }).named("secureAdminFilter");

        // make "customer" and "company" available for all templates
        GET(securePaths(), (routeContext) -> {
            routeContext.setLocal(CUSTOMER, getCustomer(routeContext));
            routeContext.setLocal(COMPANY, getCompany(routeContext));

            routeContext.next();
        }).named("customerCompanyFilter");

        // make "user" available for all templates
        GET(secureAdminPaths(), (routeContext) -> {
            routeContext.setLocal(USER, getUser(routeContext));

            routeContext.next();
        }).named("customerCompanyFilter");

        // debtor filter
        GET(securePaths(), (routeContext) -> {
            Customer customer = getCustomer(routeContext);
            if (customer.isDebtor()) {
                routeContext.render("debtor");
            } else {
                routeContext.next();
            }
        }).named("debtorFilter");

        /*
         * Register a CSRF token generator and validator.
         * This creates a session for all matching requests.
         */
        ALL(csrfAdminPaths(), new CSRFHandler()).named("CSRF handler");

        // SECURITY

        GET("/login", (routeContext) -> {
            boolean captcha = needCaptcha(routeContext.getRequest());
            routeContext.setLocal("captcha", captcha);
            if (captcha) {
                String captchaSiteKey = getPippoSettings().getString("captcha.site.key", "");
                routeContext.setLocal("captchaSiteKey", captchaSiteKey);
            }
            routeContext.render("login");
        }).named("login");

        POST("/login", (routeContext) -> {
            String email = routeContext.getParameter("email").toString();
            String password = routeContext.getParameter("password").toString();

            Customer customer = null;

            boolean captcha = needCaptcha(routeContext.getRequest());
            boolean captchaResult = false;
            if (captcha) {
                String recaptcha = routeContext.getParameter("g-recaptcha-response").toString();
                String captchaSecretKey = getPippoSettings().getString("captcha.secret.key", "");
                captchaResult = RecaptchaUtils.verify(recaptcha, captchaSecretKey);
                log.debug("Captcha result is '{}'", captchaResult);

                if (captchaResult) {
                    customer = authenticateCustomer(email, password);
                }
            } else {
                customer = authenticateCustomer(email, password);
            }

            if (customer != null) {
                routeContext.resetSession();
                resetFailedLogin(routeContext.getRequest());
                log.debug("Authenticated customer {}", customer);

                routeContext.setSession(CUSTOMER, customer);
                routeContext.redirect("/");
            } else {
                incrementFailedLogin(routeContext.getRequest());

                String errorMessage = null;
                if (captcha && !captchaResult) {
                    errorMessage = getMessages().get("login.captcha.invalid", routeContext);
                } else {
                    errorMessage = getMessages().get("login.invalid", routeContext);

                }
                routeContext.flashError(errorMessage);
                routeContext.redirect("/login");
            }
        });

        GET("/logout", (routeContext) -> {
            routeContext.resetSession();
            routeContext.redirect("/login");
        }).named("logout");

        // UPLOAD

        GET("/upload", (routeContext) -> {
            Calendar calendar = Calendar.getInstance();
//            YearMonth date = getDate(routeContext);
            YearMonth date = YearMonth.now().minusMonths(1);

            Document example = new Document()
                .setCompanyId(getCompany(routeContext).getId())
                .setYear(date.getYear())
                .setMonth(date.getMonthValue())
                .setType(Document.IN_TYPE);

            List<Document> documents = documentService.findByExample(example);

            routeContext.setLocal("documents", documents);
            routeContext.setLocal(DATE, date.format(DATE_TIME_FORMATTER));
            routeContext.setLocal("dayOfMonth", calendar.get(Calendar.DAY_OF_MONTH));
            routeContext.setLocal("lockDate", Boolean.TRUE);

            routeContext.render("upload");
        }).named("upload");

        POST("/upload", (routeContext) -> {
            FileItem file = routeContext.getRequest().getFile("files");
            long companyId = getCustomer(routeContext).getCompanyId();
            YearMonth date = getDate(routeContext);

            uploadFile(file, companyId, date, Document.IN_TYPE);

            // send response
            // we must send a json http://plugins.krajee.com/file-input#ajax-uploads
            routeContext.json().send("{}");
        });

        // STORAGE

        GET("/storage", (routeContext) -> {
            long companyId = getCustomer(routeContext).getCompanyId();
            YearMonth date = getDate(routeContext);

            Document example = new Document()
                .setCompanyId(companyId)
                .setYear(date.getYear())
                .setMonth(date.getMonthValue())
                .setType(Document.OUT_TYPE);

            List<Document> documents = documentService.findByExample(example);

            routeContext.setLocal("documents", documents);
            routeContext.setLocal("date", date.format(DATE_TIME_FORMATTER));

            routeContext.render("storage");
        }).named("storage");

        GET("/download", (routeContext) -> {
            long companyId = getCustomer(routeContext).getCompanyId();
            YearMonth date = getDate(routeContext);

            File zip = createZip(companyId, date, Document.OUT_TYPE);

            // send the zip file
            routeContext.getResponse().file(zip);
        }).named("download");

        // ADMIN

        GET("/admin/login", (routeContext) -> routeContext.render("admin/login")).named("adminLogin");

        POST("/admin/login", (routeContext) -> {
            String username = routeContext.getParameter("username").toString();
            String password = routeContext.getParameter("password").toString();

            User user = authenticateUser(username, password);

            if (user != null) {
                routeContext.resetSession();
                log.debug("Authenticated user '{}'", user);
                routeContext.setSession(USER, user);
                routeContext.redirect("/admin");
            } else {
                routeContext.flashError(getMessages().get("admin.login.invalid", routeContext));
                routeContext.redirect("/admin/login");
            }
        });

        GET("/admin/logout", (routeContext) -> {
            routeContext.resetSession();
            routeContext.redirect("/admin/login");
        }).named("adminLogout");

        GET("/admin", new RedirectHandler("/admin/storage"));

        // CUSTOMERS

        GET("/admin/customers", (routeContext) -> {
//            routeContext.setLocal("companyService", companyService); // bug in pebble
            // workaround bug
            List<CustomerDto> customers = new ArrayList<>();
            for (Customer customer : customerService.findAll()) {
                customers.add(new CustomerDto(customer, companyService.findById(customer.getCompanyId())));
            }
            routeContext.setLocal("customers", customers);
            routeContext.render("admin/customers");
        }).named("getCustomers");

        GET("/admin/customer/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            Customer customer = (id > 0) ? customerService.findById(id) : new Customer();
            routeContext.setLocal(CUSTOMER, customer);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            routeContext.setLocal("saveUrl", getRouter().uriFor("postCustomer", parameters));
            routeContext.setLocal("backUrl", getRouter().uriFor("getCustomers", new HashMap<>()));
            routeContext.setLocal("companies", companyService.findAll());

            routeContext.render("admin/customer");
        });

        POST("/admin/customer", (routeContext) -> {
            Customer entity = routeContext.createEntityFromParameters(Customer.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            customerService.save(entity);

            routeContext.getResponse().header("X-IC-Transition", "none");
//            routeContext.setLocal("customers", customerService.findAll());
            // workaround bug
            List<CustomerDto> customers = new ArrayList<>();
            for (Customer customer : customerService.findAll()) {
                customers.add(new CustomerDto(customer, companyService.findById(customer.getCompanyId())));
            }
            routeContext.setLocal("customers", customers);

            routeContext.redirect("getCustomers", new HashMap<>());
        }).named("postCustomer");

        DELETE("/admin/customer/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            customerService.deleteById(id);

            routeContext.getResponse().header("X-IC-Remove", "true").commit();
        });

        // COMPANIES

        GET("/admin/companies", (routeContext) -> {
            routeContext.setLocal("companies", companyService.findAll());
            routeContext.render("admin/companies");
        }).named("getCompanies");

        GET("/admin/company/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            Company company = (id > 0) ? companyService.findById(id) : new Company();
            routeContext.setLocal(COMPANY, company);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            routeContext.setLocal("saveUrl", getRouter().uriFor("postCompany", parameters));
            routeContext.setLocal("backUrl", getRouter().uriFor("getCompanies", new HashMap<>()));

            routeContext.render("admin/company");
        });

        POST("/admin/company", (routeContext) -> {
            Company entity = routeContext.createEntityFromParameters(Company.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            companyService.save(entity);

            routeContext.redirect("getCompanies", new HashMap<>());
        }).named("postCompany");

        // USERS

        GET("/admin/user/{id}", (routeContext) -> {
            long id = routeContext.getParameter("id").toLong();
            User user = (id > 0) ? userService.findById(id) : new User();
            routeContext.setLocal(USER, user);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            routeContext.setLocal("saveUrl", getRouter().uriFor("postUser", parameters));
            routeContext.setLocal("backUrl", "/admin");

            routeContext.render("admin/user");
        });

        POST("/admin/user", (routeContext) -> {
            User entity = routeContext.createEntityFromParameters(User.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            // for now, allow only edit user
            if ((entity.getId() != null) && (entity.getId() == 1)) {
                userService.save(entity);
            }

            routeContext.redirect("/admin");
        }).named("postUser");

        // UPLOAD ADMIN

        GET("/admin/upload", (routeContext) -> {
            Long companyId = getCompanyId(routeContext);
            YearMonth date = getDate(routeContext);

            // for filter
            routeContext.setLocal("companies", companyService.findAll());
            routeContext.setLocal(COMPANY_ID, companyId);
            routeContext.setLocal(DATE, date.format(DATE_TIME_FORMATTER));

            if (companyId == null) {
                routeContext.getResponse().getFlash().error(getMessages().get("admin.company.required", routeContext));
            } else {
                Document example = new Document()
                    .setCompanyId(companyId)
                    .setYear(date.getYear())
                    .setMonth(date.getMonthValue())
                    .setType(Document.OUT_TYPE);

                List<Document> documents = documentService.findByExample(example);

                routeContext.setLocal("documents", documents);
            }

            routeContext.render("admin/upload");
        }).named("adminUpload");

        POST("/admin/upload", (routeContext) -> {
            FileItem file = routeContext.getRequest().getFile("files");

            Long companyId = getCompanyId(routeContext);
            YearMonth date = getDate(routeContext);

            uploadFile(file, companyId, date, Document.OUT_TYPE);

            // send response
            // we must send a json http://plugins.krajee.com/file-input#ajax-uploads
            routeContext.json().send("{}");
        });

        // STORAGE ADMIN

        GET("/admin/storage", (routeContext) -> {
            Long companyId = getCompanyId(routeContext);
            YearMonth date = getDate(routeContext);

            List<Document> documents = null;
            if (companyId != null) {
                Document example = new Document()
                    .setCompanyId(companyId)
                    .setYear(date.getYear())
                    .setMonth(date.getMonthValue())
                    .setType(Document.IN_TYPE);

                documents = documentService.findByExample(example);
            }

            routeContext.setLocal("documents", documents);

            // for filter
            routeContext.setLocal("companies", companyService.findAll());
            routeContext.setLocal(COMPANY_ID, companyId);
            routeContext.setLocal(DATE, date.format(DATE_TIME_FORMATTER));

            if (companyId == null) {
                routeContext.getResponse().getFlash().error(getMessages().get("admin.company.required", routeContext));
            }

            routeContext.render("admin/storage");
        }).named("storage");

        GET("/admin/download", (routeContext) -> {
            Long companyId = getCompanyId(routeContext);
            YearMonth date = getDate(routeContext);

            File zip = createZip(companyId, date, Document.IN_TYPE);

            // send the zip file
            routeContext.getResponse().file(zip);
        }).named("adminDownload");

        // AFTER FILTERS

        ALL("/.*", (routeContext) -> {
            DbUtils.closeDb();
        }).runAsFinally();
    }

    private File createZip(long companyId, YearMonth date, String documentType) {
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

    private void uploadFile(FileItem file, long companyId, YearMonth date, String type) {
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

    private void createServices() {
        ServiceFactory serviceFactory = new ServiceFactory();

        customerService = serviceFactory.createCustomerService();
        companyService = serviceFactory.createCompanyService();
        userService = serviceFactory.createUserService();
        documentService = serviceFactory.createDocumentService();

        storage = new FileSystemStorage(UPLOAD_LOCATION);
    }

    private Customer authenticateCustomer(String email, String password) {
        if (StringUtils.isNullOrEmpty(email) || StringUtils.isNullOrEmpty(password)) {
            return null;
        }

        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            log.debug("Cannot found customer with email '{}'", email);
        } else if (!password.equals(customer.getPassword())) {
            log.debug("Password mismatch");
            customer = null;
        }

        return customer;
    }

    private User authenticateUser(String username, String password) {
        if (StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password)) {
            return null;
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            log.debug("Cannot found user with username '{}'", username);
        } else if (!password.equals(user.getPassword())) {
            log.debug("Password mismatch");
            user = null;
        }

        return user;
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

    private boolean needCaptcha(Request request) {
        String remoteHost = NetUtils.getRemoteHost(request);
        Integer failedAttempts = failedLoginByHost.get(remoteHost);

        return (failedAttempts != null) && (failedAttempts >= 5);
    }

    private void incrementFailedLogin(Request request) {
        String remoteHost = NetUtils.getRemoteHost(request);
        Integer failedAttempts = failedLoginByHost.get(remoteHost);
        if (failedAttempts == null) {
            failedLoginByHost.put(remoteHost, 1);
        } else {
            failedAttempts++;
            failedLoginByHost.put(remoteHost, failedAttempts);
        }

        log.error("Failed login IP:{} failed_attempts={}", remoteHost, failedAttempts);
    }

    private void resetFailedLogin(Request request) {
        String remoteHost = NetUtils.getRemoteHost(request);
        if (failedLoginByHost.containsKey(remoteHost)) {
            failedLoginByHost.remove(remoteHost);
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

    public User getUser(RouteContext routeContext) {
        return routeContext.getSession(USER);
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

}
