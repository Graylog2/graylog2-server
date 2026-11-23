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
import type { QueryKey } from '@tanstack/react-query';
import isPlainObject from 'lodash/isPlainObject';

import * as JSON from 'util/json';

// Mirrors react-query's default `hashKey`, but supports `BigInt` values, which we get when deserializing big numbers.
const hashQueryKey = (queryKey: QueryKey) =>
  JSON.stringify(queryKey, (_key, value) =>
    isPlainObject(value)
      ? Object.fromEntries(
          Object.keys(value)
            .sort()
            .map((key) => [key, value[key]]),
        )
      : value,
  );

export default hashQueryKey;
