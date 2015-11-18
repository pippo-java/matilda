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
package ro.fortsoft.matilda.domain;

import com.iciql.Iciql.IQColumn;
import com.iciql.Iciql.IQTable;
import ro.fortsoft.dada.iciql.IciqlEntity;

import java.util.Date;

/**
 * @author Decebal Suiu
 */
@IQTable(inheritColumns = true)
public class User extends IciqlEntity {

    @IQColumn
    private String username;

    @IQColumn
    private String password;

    @IQColumn
    private String displayName;

    @IQColumn
    private Date createdDate;

    public User() {
    }

    public User(Long id) {
        super(id);
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;

        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;

        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public User setDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public User setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;

        return this;
    }

    @Override
    public String toString() {
        return "User{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", displayName='" + displayName + '\'' +
            ", createdDate='" + createdDate + '\'' +
            '}';
    }

}
