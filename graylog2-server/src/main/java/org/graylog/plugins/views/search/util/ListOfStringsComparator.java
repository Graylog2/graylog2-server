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
package org.graylog.plugins.views.search.util;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator of lists of strings, with following features:
 * - shorter lists always come before longer lists,
 * - if lists have the same sizes, the first non-equal element decides which list goes first,
 * - comparison of elements is case-insensitive.
 */
public class ListOfStringsComparator implements Comparator<List<String>> {

    @Override
    public int compare(final List<String> o1, final List<String> o2) {

        if (o1.size() < o2.size()) {
            return -1;
        } else if (o1.size() > o2.size()) {
            return 1;
        } else {
            int index = 0;
            while (index < o1.size()) {
                String item1 = o1.get(index);
                String item2 = o2.get(index++);
                final int comparisonResult = item1.compareToIgnoreCase(item2);
                if (comparisonResult != 0) {
                    return comparisonResult;
                }
            }
            return 0;
        }
    }

}
