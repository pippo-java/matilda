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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.util.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author Decebal Suiu
 */
public class RecaptchaUtils {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaUtils.class);

    public static boolean verify(String recaptchaResponse, String secret) {
        if (StringUtils.isNullOrEmpty(recaptchaResponse)) {
            return false;
        }

        boolean result = false;
        try {
            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // add request header
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String postParams = "secret=" + secret + "&response=" + recaptchaResponse;
//            log.debug("Post parameters '{}'", postParams);

            // send post request
            connection.setDoOutput(true);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(postParams);
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            log.debug("Response code '{}'", responseCode);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // print result
            log.debug("Response '{}'", response.toString());

            // parse JSON response and return 'success' value
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.toString());
            JsonNode nameNode = rootNode.path("success");

            result = nameNode.asBoolean();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

}
