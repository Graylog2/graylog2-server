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
import { useEffect } from 'react';

import type { Location } from 'routing/withLocation';
import withLocation from 'routing/withLocation';
import { useSyncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import { ViewStore } from 'views/stores/ViewStore';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';

const useBindSearchParamsFromQuery = (query: { [key: string]: unknown }) => {
  useEffect(() => {
    const { view } = ViewStore.getInitialState();

    bindSearchParamsFromQuery({ view, query, retry: () => Promise.resolve() });
  }, [query]);
};

type Props = {
  location: Location,
};

const SynchronizeUrl = ({ location }: Props) => {
  const { pathname, search } = location;
  const query = `${pathname}${search}`;
  useBindSearchParamsFromQuery(location.query);
  useSyncWithQueryParameters(query);

  return <></>;
};

export default withLocation(SynchronizeUrl);
