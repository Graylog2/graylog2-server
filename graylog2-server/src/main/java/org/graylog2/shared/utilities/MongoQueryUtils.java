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
package org.graylog2.shared.utilities;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.mongojack.DBQuery;

import java.util.Set;

public class MongoQueryUtils {

    public static Set<Set<String>> getQueryCombinations(Set<String> query) {
        final ImmutableSet.Builder<Set<String>> builder = ImmutableSet.builder();

        for (int i = 0; i <= query.size(); i++) {
            builder.addAll(Sets.combinations(query, i));
        }
        return builder.build();
    }

    // MongoDB is not capable of comparing whether one array is contained in another one.
    // With e.g. PostgreSQL this would have been a simple '[ A,B,C ] @> [ A,B ]' query.
    // Instead we have to expand this query manually, into each possible combination. ¯\_(ツ)_/¯
    // Furthermore, MongoDB does not provide an exact "$all" query that ignores the order,
    // but matches exactly (╯°□°）╯︵ ┻━┻
    // This needs to be checked with an additional "$size" query.
    // And finally, "$all" does not work with an empty array. This needs to be compared with "$eq"
    //
    // So checking whether [A,B,C] is contained will need the following query:
    // { "$or" : [
    //   { "$or" : [ { "constraints" : { "$exists" : false } }, { "constraints" : { "$eq" : [] } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ A,B,C ] } }, { "constraints" : { "$size" : 3 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ A,B ] } }, { "constraints" : { "$size" : 2 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ A,C ] } }, { "constraints" : { "$size" : 2 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ B,C ] } }, { "constraints" : { "$size" : 2 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ A ] } }, { "constraints" : { "$size" : 1 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ B ] } }, { "constraints" : { "$size" : 1 } } ] }
    //   { "$and" : [ { "constraints" : { "$all" : [ C ] } }, { "constraints" : { "$size" : 1 } } ] }
    //   ] }
    //
    // TODO Once we can assume to run on a non-EOL MongoDB version (>=4.2) this can probably
    // TODO be replaced with an update inside an aggregation which uses the "$setIsSubset" operator.
    public static DBQuery.Query getArrayIsContainedQuery(String fieldName, Set<String> queryInput) {
        final DBQuery.Query[] expressions = getQueryCombinations(queryInput).stream().map(subset -> {
            if (subset.size() == 0) {
                // an "$all" query with an empty array never matches
                return DBQuery.or(DBQuery.notExists(fieldName), DBQuery.is(fieldName, subset));
            }
            return DBQuery.and(
                    DBQuery.all(fieldName, subset),
                    DBQuery.size(fieldName, subset.size())
            );
        }).toArray(DBQuery.Query[]::new);

        return DBQuery.or(expressions);
    }
}
