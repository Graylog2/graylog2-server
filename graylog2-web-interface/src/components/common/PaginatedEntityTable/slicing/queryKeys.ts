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

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

export const SLICING_QUERY_KEY = 'slicing';

export const slicesQueryKeyForColumn = (sliceCol: string | undefined) => [SLICING_QUERY_KEY, sliceCol] as const;

export const slicesQueryKey = (sliceCol: string | undefined, query: string | undefined, filters: UrlQueryFilters) =>
  [...slicesQueryKeyForColumn(sliceCol), query, filters] as const;
