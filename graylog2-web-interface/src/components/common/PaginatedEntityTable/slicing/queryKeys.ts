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

// The table and layout variant come last, so slices can still be invalidated by column for all tables.
export const slicesQueryKey = (
  sliceCol: string | undefined,
  query: string | undefined,
  filters: UrlQueryFilters,
  entityTableId: string | undefined,
  layoutVariant: string | undefined,
) => [...slicesQueryKeyForColumn(sliceCol), query, filters, entityTableId, layoutVariant] as const;
