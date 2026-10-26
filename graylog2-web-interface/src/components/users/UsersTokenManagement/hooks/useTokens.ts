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
import { TokenUsage } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';

type TokenUsageDTO = Awaited<ReturnType<typeof TokenUsage.getPage>>['elements'][number];

export type Token = TokenUsageDTO & { id: string };

export const fetchTokens = (searchParams: SearchParams) =>
  TokenUsage.getPage(
    searchParams.sort.attributeId as Parameters<typeof TokenUsage.getPage>[0],
    searchParams.page,
    searchParams.pageSize,
    searchParams.query || undefined,
    searchParams.sort.direction,
  ).then(({ elements, query, attributes, total, pagination: { count, page, per_page: perPage } }) => ({
    list: elements.map((el) => ({ ...el, id: el._id })),
    attributes,
    pagination: {
      count,
      total,
      page,
      perPage,
      query,
    },
  }));

export const keyFn = (searchParams: SearchParams) => ['token-management', 'overview', searchParams];
