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
package ro.fortsoft.matilda.util;

import com.iciql.Db;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.matilda.upgrade.EntitiesUpgrader;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;

import javax.sql.DataSource;

/**
 * @author Decebal Suiu
 */
public class DbUtils {

    private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

//    private static final String url = "jdbc:h2:mem:matilda";
    private static final String url = "jdbc:h2:./data/matilda";
    private static final String username = "";
    private static final String password = "";

    private static DataSource dataSource;
    private static Db genericDb;

    static {
        dataSource = JdbcConnectionPool.create(url, username, password);
    }

    public static Db getDb() {
        RouteContext routeContext = getRouteContext();
        if (routeContext == null) {
            if (genericDb == null) {
                log.debug("Create generic Db instance");
                genericDb = createDb();
            }

            return genericDb;
        }

        Db db = routeContext.getLocal("db");
        if (db == null) {
            log.debug("Create request Db instance");
            db = createDb();
            routeContext.setLocal("db", db);
        }

        return db;
    }

    public static void closeDb() {
        RouteContext routeContext = getRouteContext();
        if (routeContext == null) {
            if (genericDb != null) {
                log.debug("Close generic Db instance");
                genericDb.close();
            }
        } else {
            Db db = routeContext.removeLocal("db");
            if (db != null) {
                log.debug("Close request Db instance");
                db.close();
            }
        }
    }

    private static RouteContext getRouteContext() {
        return RouteDispatcher.getRouteContext();
    }

    private static Db createDb() {
        Db db = Db.open(dataSource);
        db.setDbUpgrader(new EntitiesUpgrader());

        return db;
    }

}
