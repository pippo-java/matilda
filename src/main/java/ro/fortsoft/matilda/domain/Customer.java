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
public class Customer extends IciqlEntity {

    @IQColumn
    private String emailAddress;

    @IQColumn
    private String firstName;

    @IQColumn
    private String lastName;

    @IQColumn
    private String password;

    @IQColumn
    private Long companyId;

    @IQColumn
    private boolean debtor;

    @IQColumn
    private boolean uploadAnytime;

    @IQColumn
    private Date createdDate;

    public Customer() {
    }

    public Customer(Long id) {
        super(id);
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Customer setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;

        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Customer setFirstName(String firstName) {
        this.firstName = firstName;

        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Customer setLastName(String lastName) {
        this.lastName = lastName;

        return this;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Customer setCompanyId(Long companyId) {
        this.companyId = companyId;

        return this;
    }

    public String getPassword() {
        return password;
    }

    public Customer setPassword(String password) {
        this.password = password;

        return this;
    }

    public boolean isDebtor() {
        return debtor;
    }

    public Customer setDebtor(boolean debtor) {
        this.debtor = debtor;

        return this;
    }

    public boolean isUploadAnytime() {
        return uploadAnytime;
    }

    public void setUploadAnytime(boolean uploadAnytime) {
        this.uploadAnytime = uploadAnytime;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Customer setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;

        return this;
    }

    @Override
    public String toString() {
        return "Customer{" +
            "id=" + id +
            ", emailAddress='" + emailAddress + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", companyId='" + companyId + '\'' +
            ", debtor='" + debtor + '\'' +
            ", uploadAnytime='" + uploadAnytime + '\'' +
            ", createdDate='" + createdDate + '\'' +
            '}';
    }

}
