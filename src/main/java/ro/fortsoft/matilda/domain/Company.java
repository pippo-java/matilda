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
public class Company extends IciqlEntity {

    @IQColumn
    private String name;

    @IQColumn
    private String fiscalCode; // CUI or CIF

    @IQColumn
    private Date createdDate;

    public Company() {
    }

    public Company(Long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public Company setName(String name) {
        this.name = name;

        return this;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public Company setFiscalCode(String fiscalCode) {
        this.fiscalCode = fiscalCode;

        return this;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Company setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;

        return this;
    }

    @Override
    public String toString() {
        return "Company{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", fiscalCode='" + fiscalCode + '\'' +
            ", createdDate='" + createdDate + '\'' +
            '}';
    }

}
