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
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import EntityItem from 'components/welcome/EntityListItem';
import { NoSearchResult, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useLastOpened from 'components/welcome/hooks/useLastOpened';

const LastOpenList = () => {
  const { data: { lastOpened }, isFetching } = useLastOpened(DEFAULT_PAGINATION);

  if (isFetching) return <Spinner />;

  if (lastOpened.length === 0) {
    return (
      <NoSearchResult>
        You have not opened any searches/dashboards yet.
        <br />
        From now on, whenever you open a saved search/dashboard, it will show up here.
        In the meantime, you can start a new <Link to={Routes.SEARCH}>Search</Link> or <Link to={Routes.pluginRoute('DASHBOARDS_NEW')}>Dashboard</Link>.
      </NoSearchResult>
    );
  }

  return (
    <ListGroup>
      {lastOpened.map(({ grn, title, timestamp }) => <EntityItem key={grn} grn={grn} title={title} timestamp={timestamp} />)}
    </ListGroup>
  );
};

export default LastOpenList;
