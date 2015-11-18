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

import ro.pippo.core.Request;
import ro.pippo.core.util.StringUtils;

/**
 * @author Decebal Suiu
 */
public class NetUtils {

    public static String getRemoteHost(Request request) {
        String remoteHost = request.getHeader("X-Forwarded-For"); // from proxy
        if (StringUtils.isNullOrEmpty(remoteHost)) {
            remoteHost = request.getHttpServletRequest().getRemoteHost();
        } else {
            // 192.168.16.75, 83.166.201.241
            int index = remoteHost.lastIndexOf(',');
            if (index != -1) {
                remoteHost = remoteHost.substring(index + 1).trim();
            }
        }

        return remoteHost;
    }

}
