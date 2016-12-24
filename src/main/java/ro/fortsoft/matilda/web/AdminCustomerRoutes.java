package ro.fortsoft.matilda.web;

import ro.fortsoft.matilda.MatildaApplication;
import ro.fortsoft.matilda.domain.Company;
import ro.fortsoft.matilda.domain.Customer;
import ro.fortsoft.matilda.domain.CustomerDto;
import ro.pippo.core.route.RouteGroup;
import ro.pippo.core.route.Router;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Decebal Suiu
 */
class AdminCustomerRoutes extends RouteGroup {

    public AdminCustomerRoutes() {
        super("/customers");

        GET("/", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

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

        GET("/{id}", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            long id = routeContext.getParameter("id").toLong();
            Customer customer;
            if (id > 0) {
                customer = application.getCustomerService().findById(id);
            } else {
                customer = new Customer();
            }
            routeContext.setLocal(MatildaApplication.CUSTOMER, customer);

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

        POST("/", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

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

        DELETE("/{id}", routeContext -> {
            MatildaApplication application = routeContext.getApplication();

            long id = routeContext.getParameter("id").toLong();
            application.getCustomerService().deleteById(id);

            routeContext.getResponse().header("X-IC-Remove", "true").commit();
        });
    }

}
