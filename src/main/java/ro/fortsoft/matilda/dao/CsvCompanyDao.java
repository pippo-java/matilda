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
package ro.fortsoft.matilda.dao;

import ro.fortsoft.dada.csv.CsvEntityDao;
import ro.fortsoft.matilda.domain.Company;
import ro.pippo.core.util.StringUtils;

import java.util.List;

/**
 * @author Decebal Suiu
 */
public class CsvCompanyDao extends CsvEntityDao<Company> implements CompanyDao {

    public CsvCompanyDao() {
        super("companies.csv");
    }

    public CsvCompanyDao(List<Company> defaults) {
        super("companies.csv", defaults);
    }

    public CsvCompanyDao(List<Company> defaults, boolean cleanOnStart) {
        super("companies.csv", defaults, cleanOnStart);
    }

    @Override
    public Company findByFiscalCode(String fiscalCode) {
        if (!StringUtils.isNullOrEmpty(fiscalCode)) {
            for (Company company : findAll()) {
                if (fiscalCode.equals(company.getFiscalCode())) {
                    return company;
                }
            }
        }

        return null;
    }

}
