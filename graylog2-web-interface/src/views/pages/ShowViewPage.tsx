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
import { useMemo } from 'react';
import type { ParsedQs } from 'qs';

import useParams from 'routing/useParams';
import useQuery from 'routing/useQuery';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import { createFromFetchError } from 'logic/errors/ReportedErrors';

import SearchPage from './SearchPage';

const useFetchView = (viewId: string, query: ParsedQs) => {
  const viewJsonPromise = useMemo(() => ViewManagementActions.get(viewId), [viewId]);

  return useMemo(() => viewJsonPromise.then((viewJson) => ViewDeserializer(viewJson, query), (error) => {
    if (error.status === 404) {
      ErrorsActions.report(createFromFetchError(error));
    }

    throw error;
  }), [query, viewJsonPromise]);
};

const ShowViewPage = () => {
  const query = useQuery();
  const { viewId } = useParams<{ viewId?: string }>();

  if (!viewId) {
    throw new Error('No view id specified!');
  }

  const view = useFetchView(viewId, query);

  return <SearchPage view={view} />;
};

export default ShowViewPage;
