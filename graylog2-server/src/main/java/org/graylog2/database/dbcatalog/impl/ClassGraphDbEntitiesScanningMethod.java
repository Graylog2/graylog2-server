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
package org.graylog2.database.dbcatalog.impl;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.graylog2.database.DbEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.shared.plugins.ChainingClassLoader;

import java.util.LinkedList;
import java.util.List;

public class ClassGraphDbEntitiesScanningMethod implements DbEntitiesScanningMethod {

    @Override
    public DbEntitiesCatalog scan(final String[] packagesToScan, final ChainingClassLoader chainingClassLoader) {

        List<DbEntityCatalogEntry> dbEntities = new LinkedList<>();
        ClassGraph classGraph = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packagesToScan);
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
                AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(annotationName);
                AnnotationParameterValueList paramVals = annotationInfo.getParameterValues();
                dbEntities.add(new DbEntityCatalogEntry(
                        paramVals.get("collection").getValue().toString(),
                        paramVals.get("titleField").getValue().toString(),
                        classInfo.loadClass(),
                        paramVals.get("readPermission").getValue().toString()
                ));
            }

            return new DbEntitiesCatalog(dbEntities);
        }
    }


}
