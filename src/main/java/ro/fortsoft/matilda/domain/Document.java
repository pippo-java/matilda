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
public class Document extends IciqlEntity {

    public static final String IN_TYPE = "in";
    public static final String OUT_TYPE = "out";

    @IQColumn
    private String name;

    @IQColumn
    private Long companyId;

    @IQColumn
    private int year;

    @IQColumn
    private int month;

    @IQColumn
    private Date uploadedDate;

    @IQColumn
    private long size;

    @IQColumn
    private String type; // IN | OUT

    public Document() {
    }

    public Document(Long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public Document setName(String name) {
        this.name = name;

        return this;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Document setCompanyId(Long companyId) {
        this.companyId = companyId;

        return this;
    }

    public int getYear() {
        return year;
    }

    public Document setYear(int year) {
        this.year = year;

        return this;
    }

    public int getMonth() {
        return month;
    }

    public Document setMonth(int month) {
        this.month = month;

        return this;
    }

    public Date getUploadedDate() {
        return uploadedDate;
    }

    public Document setUploadedDate(Date uploadedDate) {
        this.uploadedDate = uploadedDate;

        return this;
    }

    public long getSize() {
        return size;
    }

    public Document setSize(long size) {
        this.size = size;

        return this;
    }

    public String getType() {
        return type;
    }

    public Document setType(String type) {
        this.type = type;

        return this;
    }

    @Override
    public String toString() {
        return "Document{" +
            "name='" + name + '\'' +
            ", companyId=" + companyId +
            ", year=" + year +
            ", month=" + month +
            ", uploadedDate=" + uploadedDate +
            ", size=" + size +
            ", type='" + type + '\'' +
            '}';
    }

}
