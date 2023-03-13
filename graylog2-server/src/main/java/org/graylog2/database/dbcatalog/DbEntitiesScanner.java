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
package org.graylog2.database.dbcatalog;

import com.google.inject.Provider;
import org.graylog2.database.DbEntity;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

public class DbEntitiesScanner implements Provider<DbEntitiesCatalog> {

    @Override
    public DbEntitiesCatalog get() {
        final ConfigurationBuilder configuration = new ConfigurationBuilder()
                .forPackages("org.graylog2", "org.graylog")
                .setScanners(Scanners.TypesAnnotated);

        final Reflections reflections = new Reflections(configuration);

        DbEntitiesCatalog catalog = new DbEntitiesCatalog();

        reflections.getTypesAnnotatedWith(DbEntity.class).stream().forEach(
                type -> {
                    final DbEntity annotation = type.getAnnotation(DbEntity.class);
                    catalog.add(
                            new DbEntityCatalogEntry(
                                    annotation.collection(),
                                    annotation.titleField(),
                                    type)
                    );
                }
        );

        return catalog;
    }


}
