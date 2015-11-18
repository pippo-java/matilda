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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Decebal Suiu
 */
public class ZipUtils {

    /**
     * Zips every file in and under the given directory.
     */
    public static File zip(String zipFileName, Map<String, InputStream> entries) throws IOException {
        // create byte buffer
        byte[] buffer = new byte[1024];

        File zipFile = new File(zipFileName);
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (Map.Entry<String, InputStream> entry : entries.entrySet()) {
            // begin writing a new ZIP entry, positions the stream to the start of the entry data
            zos.putNextEntry(new ZipEntry(entry.getKey()));

            InputStream input = entry.getValue();

            int length;
            while ((length = input.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();

            // close the InputStream
            input.close();
        }

        // close the ZipOutputStream
        zos.close();

        return zipFile;
    }

}
