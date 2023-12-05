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
import { useCallback } from 'react';

import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import { loadNewViewForStream } from 'views/logic/views/Actions';
import { useSearchURLQueryParams } from 'views/logic/NormalizeSearchURLQueryParams';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';
import useCreateSearch from 'views/hooks/useCreateSearch';

import SearchPage from './SearchPage';

const StreamSearchPage = () => {
  const { streamId } = useParams<{ streamId?: string }>();
  const history = useHistory();

  if (!streamId) {
    throw new Error('No stream id specified!');
  }

  const { timeRange, queryString } = useSearchURLQueryParams();
  const viewPromise = useCreateSavedSearch({ streamId, timeRange, queryString });
  const view = useCreateSearch(viewPromise);

  const _loadNewView = useCallback(() => loadNewViewForStream(history, streamId), [history, streamId]);

  return <SearchPage loadNewView={_loadNewView} view={view} isNew />;
};

export default StreamSearchPage;
