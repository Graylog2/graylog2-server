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

import React from 'react';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import EntityItem from 'components/welcome/EntityListItem';
import { useLastOpened } from 'components/welcome/hooks';
import { Spinner } from 'components/common';

const LastOpenList = () => {
  const { data: { lastOpened }, isFetching } = useLastOpened(DEFAULT_PAGINATION);

  if (isFetching) return <Spinner />;
  if (!lastOpened.length) return <i>There are no last opened items</i>;

  return (
    <ListGroup data-testid="last-opened-list">
      {lastOpened.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
    </ListGroup>
  );
};

export default LastOpenList;
