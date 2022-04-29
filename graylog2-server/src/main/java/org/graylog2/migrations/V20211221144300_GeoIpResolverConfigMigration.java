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

package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

/**
 * The Original migration had a bug which caused the <b>db_vendor_type</b>  field to always be updated--replacing any user supplied values.
 *
 * <p>
 * This update writes a completion status record upon initial successful migration, and checks for presence of such record
 * before stating any subsequent migration--aborts if record found.
 * </p>
 */
public class V20211221144300_GeoIpResolverConfigMigration extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20211221144300_GeoIpResolverConfigMigration.class);
    private static final String COLLECTION_NAME = "cluster_config";
    public static final String PAYLOAD = "payload";
    private static final String FIELD_DB_VENDOR = PAYLOAD + ".db_vendor_type";
    private static final String FIELD_DB_TYPE = PAYLOAD + ".db_type";
    private static final String FIELD_DB_PATH = PAYLOAD + ".db_path";
    private static final String FIELD_CITY_DB_PATH = PAYLOAD + ".city_db_path";
    private static final String FIELD_ASN_DB_PATH = PAYLOAD + ".asn_db_path";
    private static final String FIELD_ENFORCE = PAYLOAD + ".enforce_graylog_schema";

    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20211221144300_GeoIpResolverConfigMigration(MongoConnection mongoConnection,
                                                        ClusterConfigService clusterConfigService) {
        this.mongoConnection = mongoConnection;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-01-28t16:38Z");
    }

    /**
     * This code change modifies {@link GeoIpResolverConfig} by removing the field <b>db_type</b> and adding the field <b>database_vendor_type</b>.
     *
     * <p>
     * The objective of this migration is to add the new field (with value {@link DatabaseVendorType#MAXMIND}) if not already present, and to remove the old field.
     * </p>
     */
    @Override
    public void upgrade() {

        MigrationCompletion completion = clusterConfigService.get(MigrationCompletion.class);
        if (completion != null) {
            LOG.debug("Migration was already completed");
            return;
        }

        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        LOG.info("Updating '{}' collection.", COLLECTION_NAME);

        Bson geoConfFilter = Filters.eq("type", GeoIpResolverConfig.class.getCanonicalName());
        Bson noColumnFilter = Filters.exists(FIELD_DB_VENDOR, false);

        //set default value for 'enforce_graylog_schema'
        Bson setEnforceSchema = Updates.set(FIELD_ENFORCE, false);

        //set blank asn db path
        Bson setAsnPath = Updates.set(FIELD_ASN_DB_PATH, "");

        //rename db type field to db vendor type
        Bson renameDbTypeToVendor = Updates.rename(FIELD_DB_TYPE, FIELD_DB_VENDOR);

        //rename existing db_path field to city_db_path
        Bson renameDbPath = Updates.rename(FIELD_DB_PATH, FIELD_CITY_DB_PATH);

        Bson updates = Updates.combine(setEnforceSchema, renameDbTypeToVendor, renameDbPath, setAsnPath);
        LOG.info("Planned Updates: {}", updates);
        final UpdateResult updateResult = collection.updateOne(Filters.and(geoConfFilter, noColumnFilter), updates);
        LOG.info("Update Result: {}", updateResult);

        Bson setDefaultVendor = Updates.set(FIELD_DB_VENDOR, DatabaseVendorType.MAXMIND.name());
        LOG.info("Setting default vendor: {}", setDefaultVendor);
        final UpdateResult updateVendorResult = collection.updateOne(geoConfFilter, setDefaultVendor);
        LOG.info("Default Vendor Update Result: {}", updateVendorResult);

        clusterConfigService.write(MigrationCompletion.create());

    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class MigrationCompletion {

        @JsonCreator
        public static MigrationCompletion create() {
            return new AutoValue_V20211221144300_GeoIpResolverConfigMigration_MigrationCompletion();
        }
    }
}

