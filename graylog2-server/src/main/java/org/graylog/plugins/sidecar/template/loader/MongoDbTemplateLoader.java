/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.sidecar.template.loader;

import freemarker.cache.TemplateLoader;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class MongoDbTemplateLoader implements TemplateLoader {
    private final JacksonDBCollection<Configuration, ObjectId> dbCollection;

    public MongoDbTemplateLoader(JacksonDBCollection<Configuration, ObjectId> dbCollection) {
        this.dbCollection = dbCollection;
    }

    @Override
    public Object findTemplateSource(String id) throws IOException {
        Configuration configuration;
        try {
            configuration = dbCollection.findOne(DBQuery.is("_id", unlocalize(id)));
        } catch (IllegalArgumentException e) {
            // no ObjectID so skip MongoDB loader and try with next one
            return null;
        }
        if (configuration == null) {
            throw new IOException("Can't find template: " + unlocalize(id));
        }
        return configuration.template();
    }

    @Override
    public long getLastModified(Object o) {
        return 0;
    }

    @Override
    public Reader getReader(Object snippet, String encoding) {
        return new StringReader((String)snippet);
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
