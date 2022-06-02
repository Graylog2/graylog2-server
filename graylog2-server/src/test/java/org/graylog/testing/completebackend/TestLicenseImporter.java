package org.graylog.testing.completebackend;

import org.graylog.testing.mongodb.MongoDBInstance;

import java.util.List;

public interface TestLicenseImporter {
    default void importLicenses(final MongoDBInstance mongoDBInstance, final List<String> licenses) {}
}
