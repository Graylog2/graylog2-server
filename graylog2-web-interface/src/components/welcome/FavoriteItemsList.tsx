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

import React, { useCallback, useState } from 'react';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import EntityItem from 'components/welcome/EntityListItem';
import { NoSearchResult, PaginatedList, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useFavoriteItems from 'components/welcome/hooks/useFavoriteItems';

const FavoriteItemsList = () => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: { favorites, total }, isFetching } = useFavoriteItems(pagination);
  const onPageChange = useCallback((newPage) => {
    setPagination((cur) => ({ ...cur, page: newPage }));
  }, [setPagination]);

  if (isFetching) return <Spinner />;

  if (favorites.length === 0) {
    return (
      <NoSearchResult>
        You do not have any favorite items yet.
        <br />
        Make any <Link to={Routes.SEARCH}>Search</Link> or <Link to={Routes.pluginRoute('DASHBOARDS_NEW')}>Dashboard</Link> favorite to show up here.
      </NoSearchResult>
    );
  }

  return (
    <PaginatedList onChange={onPageChange} useQueryParameter={false} activePage={pagination.page} totalItems={total} pageSize={pagination.per_page} showPageSizeSelect={false} hideFirstAndLastPageLinks>
      <ListGroup>
        {favorites.map(({ grn, title }) => <EntityItem key={grn} grn={grn} title={title} />)}
      </ListGroup>
    </PaginatedList>
  );
};

export default FavoriteItemsList;
