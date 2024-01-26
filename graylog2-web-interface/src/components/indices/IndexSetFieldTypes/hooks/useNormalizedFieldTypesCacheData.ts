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
import { useIsFetching, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import type { IndexSetFieldTypesQueryData } from 'components/indices/IndexSetFieldTypes/types';
import getNormalizedCacheData from 'components/indices/IndexSetFieldTypes/helpers/getNormalizedCacheData';

const useNormalizedFieldTypesCacheData = () => {
  const queryClient = useQueryClient();
  const isFetching = useIsFetching(['indexSetFieldTypes']);

  return useMemo(() => {
    const queryCache = queryClient.getQueryCache().findAll(['indexSetFieldTypes']) as Array<Query<IndexSetFieldTypesQueryData>>;

    return getNormalizedCacheData(queryCache);
    // we need isFetching to recalculate data
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isFetching, queryClient]);
};

export default useNormalizedFieldTypesCacheData;
