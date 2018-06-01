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
