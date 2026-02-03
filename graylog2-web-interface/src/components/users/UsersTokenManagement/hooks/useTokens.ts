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
import type { SearchParams, Attributes } from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

export type Token = {
  id: string;
  _id: string;
  username: string;
  user_id: string;
  NAME: string;
  created_at: string;
  last_access: string;
  external_user: boolean;
  title: string;
  user_deleted: boolean;
};

type PaginatedResponse = {
  per_page: number;
  count: number;
  total: number;
  page: number;
  query: string;
  attributes: Attributes;
  elements: Array<Token>;
};

export const fetchTokens = (searchParams: SearchParams) => {
  const url = PaginationURL(
    ApiRoutes.TokenManagementController.paginated().url,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    {
      sort: searchParams.sort.attributeId,
      order: searchParams.sort.direction,
    },
  );

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedResponse) => {
    const { elements, query, attributes, count, total, page, per_page: perPage } = response;

    return {
      list: elements.map((el) => ({ ...el, id: el._id })),
      attributes,
      pagination: {
        count,
        total,
        page,
        perPage,
        query,
      },
    };
  });
};

export const keyFn = (searchParams: SearchParams) => ['token-management', 'overview', searchParams];
