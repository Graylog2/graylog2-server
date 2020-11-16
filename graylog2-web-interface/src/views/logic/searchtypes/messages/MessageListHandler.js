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
import _ from 'lodash';
import Immutable from 'immutable';

export default {
  convert(result) {
    const fieldNames = Immutable.Map().withMutations((map) => {
      _.forEach(result.messages, (msg) => {
        _.forOwn(msg.message, (value, field) => {
          // add occurrences
          map.mergeWith((oldVal, newVal) => oldVal + newVal, Immutable.Map([[field, 1]]));
        });
      });
    });

    return {
      id: result.id,
      effectiveTimerange: result.effective_timerange,
      type: result.type,
      messages: result.messages,
      total: result.total_results,
      fields: fieldNames, // computed fieldname -> occurrence count
    };
  },
};
