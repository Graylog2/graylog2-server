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

import Spinner from 'components/common/Spinner';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import { loadNewViewForStream } from 'views/logic/views/Actions';
import normalizeSearchURLQueryParams from 'views/logic/NormalizeSearchURLQueryParams';
import useQuery from 'routing/useQuery';
import useParams from 'routing/useParams';
import useLoadView from 'views/pages/useLoadView';

import SearchPage from './SearchPage';

const StreamSearchPage = () => {
  const query = useQuery();
  const { streamId } = useParams<{ streamId?: string }>();

  if (!streamId) {
    throw new Error('No stream id specified!');
  }

  const { timeRange, queryString } = normalizeSearchURLQueryParams(query);
  const newView = useCreateSavedSearch(streamId, timeRange, queryString);
  useLoadView(newView);
  const [loaded, HookComponent] = useProcessHooksForView(newView, query);

  const _loadNewView = useCallback(() => loadNewViewForStream(streamId), [streamId]);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage loadNewView={_loadNewView} />;
};

export default StreamSearchPage;
