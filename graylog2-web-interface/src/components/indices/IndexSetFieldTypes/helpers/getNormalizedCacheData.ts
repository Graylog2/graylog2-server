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

import type { Query } from '@tanstack/react-query';
import keyBy from 'lodash/keyBy';
import reduce from 'lodash/reduce';
import extend from 'lodash/extend';

import type { IndexSetFieldTypesQueryData, IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';

const getNormalizedCacheData = (queryCache: Array<Query<IndexSetFieldTypesQueryData>>): Record<string, IndexSetFieldType> => {
  const cacheData: Array<Record<string, IndexSetFieldType>> = queryCache.map(({ state }) => keyBy(state?.data?.list, 'id'));
  const mergedCache: Record<string, IndexSetFieldType> = reduce<Record<string, IndexSetFieldType>>(cacheData, extend);

  return mergedCache;
};

export default getNormalizedCacheData;
