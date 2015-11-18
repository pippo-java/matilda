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
package ro.fortsoft.matilda.service;

import ro.fortsoft.matilda.domain.Company;
import ro.fortsoft.matilda.domain.Customer;
import ro.fortsoft.matilda.domain.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class ServiceFactory {

    public CustomerService createCustomerService() {
        CustomerService customerService = new DefaultCustomerService();
        if (customerService.count() == 0) {
            // add defaults
            List<Customer> defaults = new ArrayList<>();
            defaults.add(new Customer()
                .setEmailAddress("test@test.ro")
                .setPassword("1")
                .setFirstName("Decebal")
                .setLastName("Suiu")
                .setCompanyId(1L));
            defaults.add(new Customer()
                .setEmailAddress("test2@test.ro")
                .setPassword("1")
                .setFirstName("Peter")
                .setDebtor(true)
                .setCompanyId(1L));
            defaults.add(new Customer()
                .setEmailAddress("test3@test.ro")
                .setPassword("1")
                .setFirstName("Maria")
                .setCompanyId(1L));

            for (Customer customer : defaults) {
                customerService.save(customer);
            }
        }

        return customerService;
    }

    public CompanyService createCompanyService() {
        CompanyService companyService = new DefaultCompanyService();
        if (companyService.count() == 0) {
            // add defaults
            List<Company> defaults = new ArrayList<>();
            defaults.add(new Company()
                .setName("decisoft")
                .setFiscalCode("111111111"));

            for (Company company : defaults) {
                companyService.save(company);
            }
        }

        return companyService;
    }

    public UserService createUserService() {
        UserService userService = new DefaultUserService();
        if (userService.count() == 0) {
            // add defaults
            List<User> defaults = new ArrayList<>();
            defaults.add(new User()
                .setUsername("test")
                .setPassword("1"));

            for (User user : defaults) {
                userService.save(user);
            }
        }

        return userService;
    }

    public DocumentService createDocumentService() {
        return new DefaultDocumentService();
    }

}
