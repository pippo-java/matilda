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
package ro.fortsoft.matilda.upgrade;

import com.iciql.Db;
import com.iciql.DbUpgrader;
import com.iciql.Iciql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See http://iciql.com/table_versioning.html
 *
 * @author Decebal Suiu
 */
//@Iciql.IQVersion(2)
@Iciql.IQVersion(0)
public class EntitiesUpgrader implements DbUpgrader {

    private static final Logger log = LoggerFactory.getLogger(EntitiesUpgrader.class);

    @Override
    public boolean upgradeDatabase(Db db, int fromVersion, int toVersion) {
        log.info("Upgrading database from version '{}' to version '{}'", fromVersion, toVersion);

        int currentVersion = fromVersion;

        boolean upgraded = false;
        if (currentVersion == 0) {
            V1 v1 = db.open(V1.class);
            v1.updateCompanyTable();
            v1.updateCustomerTable();
            v1.updateUserTable();
            currentVersion++;
            log.info("Upgraded database to version '{}'", currentVersion);
            upgraded = true;
        }

        if ((currentVersion == 1) && (currentVersion < toVersion)) {
            V2 v2 = db.open(V2.class);
            v2.updateUserTable();
            currentVersion++;
            log.info("Upgraded database to version '{}'", currentVersion);
            upgraded = true;
        }

        return upgraded;
    }

    @Override
    public boolean upgradeTable(Db db, String schema, String table, int fromVersion, int toVersion) {
        log.info("Upgrade table '{}' from '{}' to '{}'", table, fromVersion, toVersion);

        return false;
    }

}
