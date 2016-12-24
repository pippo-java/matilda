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
package ro.fortsoft.matilda.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.matilda.MatildaApplication;
import ro.fortsoft.matilda.domain.Customer;
import ro.fortsoft.matilda.domain.Document;
import ro.fortsoft.matilda.util.NetUtils;
import ro.fortsoft.matilda.util.RecaptchaUtils;
import ro.pippo.core.FileItem;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.Request;
import ro.pippo.core.route.RouteGroup;
import ro.pippo.core.util.StringUtils;

import java.io.File;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Decebal Suiu
 */
public class Routes extends RouteGroup {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    private final MatildaApplication application;
    private Map<String, Integer> failedLoginByHost = new ConcurrentHashMap<>();

    public Routes(MatildaApplication application) {
        super("/");

        this.application = application;

        // ROUTES
        addSecurityRoutes();
        addUploadRoutes();
        addStorageRoutes();
    }

    private void addSecurityRoutes() {
        GET("/login", routeContext -> {
            boolean captcha = needCaptcha(routeContext.getRequest());
            routeContext.setLocal("captcha", captcha);
            if (captcha) {
                String captchaSiteKey = getPippoSettings().getString("captcha.site.key", "");
                routeContext.setLocal("captchaSiteKey", captchaSiteKey);
            }
            routeContext.render("login");
        }).named("login");

        POST("/login", routeContext -> {
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

                routeContext.setSession(MatildaApplication.CUSTOMER, customer);
                routeContext.redirect("/");
            } else {
                incrementFailedLogin(routeContext.getRequest());

                String errorMessage;
                if (captcha && !captchaResult) {
                    errorMessage = getMessages().get("login.captcha.invalid", routeContext);
                } else {
                    errorMessage = getMessages().get("login.invalid", routeContext);

                }
                routeContext.flashError(errorMessage);
                routeContext.redirect("/login");
            }
        });

        GET("/logout", routeContext -> {
            routeContext.resetSession();
            routeContext.redirect("/login");
        }).named("logout");
    }

    private void addUploadRoutes() {
        GET("/upload", routeContext -> {
            Calendar calendar = Calendar.getInstance();
//            YearMonth date = getDate(routeContext);
            YearMonth date = YearMonth.now().minusMonths(1);

            Document example = new Document()
                .setCompanyId(application.getCompany(routeContext).getId())
                .setYear(date.getYear())
                .setMonth(date.getMonthValue())
                .setType(Document.IN_TYPE);

            List<Document> documents = application.getDocumentService().findByExample(example);

            routeContext.setLocal("documents", documents);
            routeContext.setLocal(MatildaApplication.DATE, date.format(MatildaApplication.DATE_TIME_FORMATTER));
            routeContext.setLocal("dayOfMonth", calendar.get(Calendar.DAY_OF_MONTH));
            routeContext.setLocal("lockDate", Boolean.TRUE);

            routeContext.render("upload");
        }).named("upload");

        POST("/upload", routeContext -> {
            FileItem file = routeContext.getRequest().getFile("files");
            long companyId = application.getCustomer(routeContext).getCompanyId();
            YearMonth date = application.getDate(routeContext);

            application.uploadFile(file, companyId, date, Document.IN_TYPE);

            // send response
            // we must send a json http://plugins.krajee.com/file-input#ajax-uploads
            routeContext.json().send("{}");
        });
    }

    private void addStorageRoutes() {
        GET("/storage", routeContext -> {
            long companyId = application.getCustomer(routeContext).getCompanyId();
            YearMonth date = application.getDate(routeContext);

            Document example = new Document()
                .setCompanyId(companyId)
                .setYear(date.getYear())
                .setMonth(date.getMonthValue())
                .setType(Document.OUT_TYPE);

            List<Document> documents = application.getDocumentService().findByExample(example);

            routeContext.setLocal("documents", documents);
            routeContext.setLocal("date", date.format(MatildaApplication.DATE_TIME_FORMATTER));

            routeContext.render("storage");
        }).named("storage");

        GET("/download", routeContext -> {
            long companyId = application.getCustomer(routeContext).getCompanyId();
            YearMonth date = application.getDate(routeContext);

            File zip = application.createZip(companyId, date, Document.OUT_TYPE);

            // send the zip file
            routeContext.getResponse().file(zip);
        }).named("download");
    }

    private PippoSettings getPippoSettings() {
        return application.getPippoSettings();
    }

    private Messages getMessages() {
        return application.getMessages();
    }

    private Customer authenticateCustomer(String email, String password) {
        if (StringUtils.isNullOrEmpty(email) || StringUtils.isNullOrEmpty(password)) {
            return null;
        }

        Customer customer = application.getCustomerService().findByEmail(email);
        if (customer == null) {
            log.debug("Cannot found customer with email '{}'", email);
        } else if (!password.equals(customer.getPassword())) {
            log.debug("Password mismatch");
            customer = null;
        }

        return customer;
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

}
