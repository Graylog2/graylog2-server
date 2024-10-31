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

import com.google.common.base.Stopwatch;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.database.DbEntity;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class DbEntitiesScanner implements Provider<DbEntitiesCatalog> {
    private static final Logger LOG = LoggerFactory.getLogger(DbEntitiesScanner.class);

    private final String[] packagesToScan;
    private final String[] packagesToExclude;
    private final ChainingClassLoader chainingClassLoader;

    @SuppressWarnings("unused")
    @Inject
    public DbEntitiesScanner(final ChainingClassLoader chainingClassLoader) {
        this.chainingClassLoader = chainingClassLoader;
        this.packagesToScan = new String[]{"org.graylog2", "org.graylog"};
        this.packagesToExclude = new String[]{"org.graylog.shaded", "org.graylog.storage", "org.graylog2.migrations"};
    }

    DbEntitiesScanner(final String[] packagesToScan, final String[] packagesToExclude) {
        this.chainingClassLoader = null;
        this.packagesToScan = packagesToScan;
        this.packagesToExclude = packagesToExclude;
    }

    @Override
    public DbEntitiesCatalog get() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final DbEntitiesCatalog catalog = scan(packagesToScan, packagesToExclude, chainingClassLoader);
        stopwatch.stop();
        LOG.info("{} entities have been scanned and added to DB Entity Catalog, it took {}", catalog.size(), stopwatch);
        return catalog;
    }

    public DbEntitiesCatalog scan(final String[] packagesToScan,
                                  final String[] packagesToExclude,
                                  final ChainingClassLoader chainingClassLoader) {
        List<DbEntityCatalogEntry> dbEntities = new LinkedList<>();
        ClassGraph classGraph = new ClassGraph()
                .enableAnnotationInfo()
                .acceptPackages(packagesToScan)
                .rejectPackages(packagesToExclude)
                .filterClasspathElements(classpathElementPathStr -> classpathElementPathStr.contains("graylog"))
                .disableRuntimeInvisibleAnnotations();

        if (chainingClassLoader != null) {
            //Unfortunately, ClassGraph does not work correctly if provided with ChainingClassLoader as a whole
            //You have to manually add all class loaders from ChainingClassLoader
            for (ClassLoader cl : chainingClassLoader.getClassLoaders()) {
                classGraph = classGraph.addClassLoader(cl);
            }
        }

        try (ScanResult scanResult = classGraph.scan()) {
            final String annotationName = DbEntity.class.getCanonicalName();
            ClassInfoList classInfoList = scanResult.getClassesWithAnnotation(annotationName);
            for (ClassInfo classInfo : classInfoList) {
                final var annotations = classInfo.getAnnotationInfoRepeatable(annotationName);
                for (final var annotationInfo : annotations) {
                    AnnotationParameterValueList paramVals = annotationInfo.getParameterValues();
                    dbEntities.add(new DbEntityCatalogEntry(
                            paramVals.get("collection").getValue().toString(),
                            paramVals.get("titleField").getValue().toString(),
                            classInfo.loadClass(),
                            paramVals.get("readPermission").getValue().toString()
                    ));
                }
            }

            return new DbEntitiesCatalog(dbEntities);
        }
    }
}
