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
import URI from 'urijs';
import { useHistory, useLocation } from 'react-router-dom';

import useQuery from 'routing/useQuery';

const usePaginationQueryParameter = () => {
  const { page } = useQuery();
  const history = useHistory();
  const { search, pathname } = useLocation();

  const query = pathname + search;

  const setPage = (newPage: number) => {
    const uri = new URI(query).setSearch('page', String(newPage));
    history.replace(uri.toString());
  };

  return { page: Number(page) || 1, setPage };
};

export default usePaginationQueryParameter;
