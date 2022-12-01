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
import { EmptyResult, PaginatedList, Spinner } from 'components/common';
import { useFavoriteItems } from 'components/welcome/hooks';

const FavoriteItemsList = () => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: { favoriteItems, total }, isFetching } = useFavoriteItems(pagination);
  const onPageChange = useCallback((newPage) => {
    setPagination((cur) => ({ ...cur, page: newPage }));
  }, [setPagination]);

  if (isFetching) return <Spinner />;

  if (favoriteItems.length === 0) {
    return (
      <EmptyResult>
        You do not have any favorite items yet.
        Star any search/dashboard for it to show up here.
      </EmptyResult>
    );
  }

  return (
    <PaginatedList onChange={onPageChange} useQueryParameter={false} activePage={pagination.page} totalItems={total} pageSize={pagination.per_page} showPageSizeSelect={false} hideFirstAndLastPageLinks>
      <ListGroup>
        {favoriteItems.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
      </ListGroup>
    </PaginatedList>
  );
};

export default FavoriteItemsList;
