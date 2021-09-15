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
import { useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { parse, stringify } from 'qs';

import { Pagination } from 'stores/PaginationTypes';

const useLocationSearchPagination = (defaultPagination: Pagination): [Pagination, (nextPagination: Pagination) => void] => {
  const history = useHistory();
  const location = useLocation();
  const [parsedPagination, setParsedPagination] = useState(defaultPagination);

  useEffect(() => {
    const convertToSafePositiveInteger = (maybeNumber: any): number | undefined => {
      const parsedNumber = Number.parseInt(maybeNumber, 10);

      return (Number.isSafeInteger(parsedNumber) && parsedNumber > 0) ? parsedNumber : undefined;
    };

    const parsePaginationFromSearch = (search: string): Pagination => {
      const parsedSearch = parse(search, { ignoreQueryPrefix: true }) ?? {};
      const { page: searchPage, perPage: searchPerPage, query: searchQuery } = parsedSearch;

      return {
        page: convertToSafePositiveInteger(searchPage) ?? defaultPagination.page,
        perPage: convertToSafePositiveInteger(searchPerPage) ?? defaultPagination.perPage,
        query: typeof searchQuery === 'string' ? searchQuery : defaultPagination.query,
      };
    };

    setParsedPagination(parsePaginationFromSearch(location.search));
  }, [location.search, defaultPagination]);

  const setLocationSearchPagination = (nextPagination: Pagination) => {
    history.push({
      pathname: location.pathname,
      search: stringify(nextPagination),
    });
  };

  return [parsedPagination, setLocationSearchPagination];
};

export default useLocationSearchPagination;
