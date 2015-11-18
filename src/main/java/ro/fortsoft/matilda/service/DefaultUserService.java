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

import ro.fortsoft.dada.core.service.BaseEntityService;
import ro.fortsoft.matilda.dao.IciqlUserDao;
import ro.fortsoft.matilda.dao.UserDao;
import ro.fortsoft.matilda.domain.User;

import java.util.Collections;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class DefaultUserService extends BaseEntityService<User> implements UserService {

    public DefaultUserService() {
        this(Collections.emptyList());
    }

    public DefaultUserService(List<User> defaults) {
//        super(new CsvUserDao(defaults));
        super(new IciqlUserDao());
    }

    @Override
    public User findByUsername(String username) {
        return getDao().findByUsername(username);
    }

    @Override
    protected UserDao getDao() {
        return (UserDao) super.getDao();
    }

}
