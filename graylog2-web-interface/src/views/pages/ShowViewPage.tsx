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
import * as React from 'react';

import useParams from 'routing/useParams';
import useFetchView from 'views/hooks/useFetchView';

import SearchPage from './SearchPage';

const ShowViewPage = ({ children }: React.PropsWithChildren<{}>) => {
  const { viewId } = useParams<{ viewId?: string }>();

  if (!viewId) {
    throw new Error('No view id specified!');
  }

  const view = useFetchView(viewId);

  return <SearchPage view={view} isNew={false}>{children}</SearchPage>;
};

export default ShowViewPage;
