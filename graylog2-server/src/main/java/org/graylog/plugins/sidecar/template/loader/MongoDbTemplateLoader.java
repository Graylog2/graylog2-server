/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.sidecar.template.loader;

import freemarker.cache.TemplateLoader;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog2.database.utils.MongoUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class MongoDbTemplateLoader implements TemplateLoader {
    private final MongoUtils<Configuration> mongoUtils;

    public MongoDbTemplateLoader(MongoUtils<Configuration> mongoUtils) {
        this.mongoUtils = mongoUtils;
    }

    @Override
    public Object findTemplateSource(String id) throws IOException {

        final ObjectId objectId;
        try {
            objectId = new ObjectId(unlocalize(id));
        } catch (IllegalArgumentException e) {
            // no ObjectID so skip MongoDB loader and try with next one
            return null;
        }

        return mongoUtils.getById(objectId)
                .map(Configuration::template)
                .orElseThrow(() -> new IOException("Can't find template: " + unlocalize(id)));
    }

    @Override
    public long getLastModified(Object o) {
        return 0;
    }

    @Override
    public Reader getReader(Object snippet, String encoding) {
        return new StringReader((String) snippet);
    }

    @Override
    public void closeTemplateSource(Object o) {
    }

    private String unlocalize(String s) {
        if (s.contains("_")) {
            return s.substring(0, s.indexOf("_"));
        } else {
            return s;
        }
    }
}
