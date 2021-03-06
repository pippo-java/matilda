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
import ro.fortsoft.matilda.dao.DocumentDao;
import ro.fortsoft.matilda.dao.IciqlDocumentDao;
import ro.fortsoft.matilda.domain.Document;

/**
 * @author Decebal Suiu
 */
public class DefaultDocumentService extends BaseEntityService<Document> implements DocumentService {

    public DefaultDocumentService() {
//        super(new CsvDocumentDao());
        super(new IciqlDocumentDao());
    }

    @Override
    protected DocumentDao getDao() {
        return (DocumentDao) super.getDao();
    }

}
