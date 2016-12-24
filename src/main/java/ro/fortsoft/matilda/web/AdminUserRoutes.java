package ro.fortsoft.matilda.web;

import ro.fortsoft.matilda.MatildaApplication;
import ro.fortsoft.matilda.domain.User;
import ro.pippo.core.route.RouteGroup;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Decebal Suiu
 */
class AdminUserRoutes extends RouteGroup {

    public AdminUserRoutes() {
        super("/users");

        GET("/{id}", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            long id = routeContext.getParameter("id").toLong();
            User user = (id > 0) ? application.getUserService().findById(id) : new User();
            routeContext.setLocal(MatildaApplication.USER, user);

            Map<String, Object> parameters = new HashMap<>();
            if (id > 0) {
                parameters.put("id", id);
            }
            routeContext.setLocal("saveUrl", application.getRouter().uriFor("postUser", parameters));
            routeContext.setLocal("backUrl", "/admin");

            routeContext.render("admin/user");
        });

        POST("/", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

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

}
