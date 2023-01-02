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
package org.graylog2.indexer.indices.util;

import java.util.Comparator;

public class NumberBasedIndexNameComparator implements Comparator<String> {

    private final String separator;

    public NumberBasedIndexNameComparator(final String separator) {
        this.separator = separator;
    }

    @Override
    public int compare(String indexName1, String indexName2) {
        int separatorPosition = indexName1.lastIndexOf(separator);
        int index1Number;
        try {
            index1Number = Integer.parseInt(indexName1.substring(separatorPosition + 1));
        } catch (Exception e) {
            index1Number = Integer.MIN_VALUE; //wrongly formatted index names go last
        }

        separatorPosition = indexName2.lastIndexOf(separator);
        int index2Number;
        try {
            index2Number = Integer.parseInt(indexName2.substring(separatorPosition + 1));
        } catch (NumberFormatException e) {
            index2Number = Integer.MIN_VALUE; //wrongly formatted index names go last
        }
        return -Integer.compare(index1Number, index2Number);
    }
}
