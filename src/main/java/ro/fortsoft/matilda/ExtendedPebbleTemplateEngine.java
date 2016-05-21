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
package ro.fortsoft.matilda;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import ro.fortsoft.matilda.util.FileUtils;
import ro.pippo.core.Application;
import ro.pippo.pebble.PebbleTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Decebal Suiu
 */
public class ExtendedPebbleTemplateEngine extends PebbleTemplateEngine {

    @Override
    protected void init(Application application, PebbleEngine.Builder builder) {
        builder.extension(new AbstractExtension() {

            @Override
            public Map<String, Filter> getFilters() {
                Map<String, Filter> filters = new HashMap<>();
                filters.put("fileSize", new FileSizeFilter());

                return filters;
            }

        });
    }

    private class FileSizeFilter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return null;
        }

        @Override
        public Object apply(Object input, Map<String, Object> args){
            return (input != null) ? FileUtils.format((Long) input) : null;
        }

    }

}
