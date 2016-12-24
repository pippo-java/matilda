package ro.fortsoft.matilda.web;

import ro.fortsoft.matilda.MatildaApplication;
import ro.fortsoft.matilda.domain.Company;
import ro.pippo.core.route.RouteGroup;
import ro.pippo.core.route.Router;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Decebal Suiu
 */
class AdminCompanyRoutes extends RouteGroup {

    public AdminCompanyRoutes() {
        super("/companies");

        GET("/", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            routeContext.setLocal("companies", application.getCompanyService().findAll());
            routeContext.render("admin/companies");
        }).named("getCompanies");

        GET("/{id}", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            long id = routeContext.getParameter("id").toLong();
            Company company = (id > 0) ? application.getCompanyService().findById(id) : new Company();
            routeContext.setLocal(MatildaApplication.COMPANY, company);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            Router router = application.getRouter();
            routeContext.setLocal("saveUrl", router.uriFor("postCompany", parameters));
            routeContext.setLocal("backUrl", router.uriFor("getCompanies", new HashMap<>()));

            routeContext.render("admin/company");
        });

        POST("/", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            Company entity = routeContext.createEntityFromParameters(Company.class);
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(new Date());
            }
            application.getCompanyService().save(entity);

            routeContext.redirect("getCompanies", new HashMap<>());
        }).named("postCompany");
    }

}
