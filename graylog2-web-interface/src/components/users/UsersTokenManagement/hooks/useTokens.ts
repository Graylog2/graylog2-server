import type {SearchParams, Attributes} from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';
import {qualifyUrl} from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

export type Token = {
  id: string,
  token_id: string,
  username: string,
  user_id: string,
  token_name: string,
  created_at: string,
  last_access: string,
  user_is_external: boolean,
  auth_backend: string
};

type PaginatedResponse = {
  per_page: number;
  count: number;
  total: number;
  page: number;
  query: string,
  attributes: Attributes,
  token_usage: Array<Token>,
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
    const {
      token_usage,
      query,
      attributes,
      count,
      total,
      page,
      per_page: perPage,
    } = response;

    return {
      list: token_usage,
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
}

export const keyFn = (searchParams: SearchParams) => ['token-management', 'overview', searchParams];
