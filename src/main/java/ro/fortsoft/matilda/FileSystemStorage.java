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

import ro.fortsoft.matilda.domain.Document;
import ro.fortsoft.matilda.util.IoUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Decebal Suiu
 */
public class FileSystemStorage implements Storage {

    private String baseDirectory;

    public FileSystemStorage(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public long store(InputStream stream, Document document) {
        File bucket = getBucket(document);
        if (!bucket.exists()) {
            bucket.mkdirs();
        }

        try {
            return IoUtils.copy(stream, getFile(document));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public long size(Document document) {
        return getFile(document).length();
    }

    @Override
    public InputStream getStream(Document document) {
        try {
            return new BufferedInputStream(new FileInputStream(getFile(document)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File getFile(Document document) {
        return new File(getBucket(document), document.getName());
    }

    private File getBucket(Document document) {
        return new File(getBucketPath(document));
    }

    private String getBucketPath(Document document) {
        return new StringBuilder()
            .append(baseDirectory)
            .append(File.separator)
            .append(document.getCompanyId())
            .append(File.separator)
            .append(document.getYear())
            .append(File.separator)
            .append(document.getMonth())
            .append(File.separator)
            .append(document.getType())
            .toString();
    }

}
