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
// @flow strict
import * as React from 'react';
import { useCallback } from 'react';

import Spinner from 'components/common/Spinner';
import withParams from 'routing/withParams';
import useLoadView from 'views/logic/views/UseLoadView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';
import { loadNewViewForStream } from 'views/logic/views/Actions';

import SearchPage from './SearchPage';

type Props = {
  location: Location,
  params: {
    streamId: ?string,
  },
};

const StreamSearchPage = ({ params: { streamId }, location: { query } }: Props) => {
  const newView = useCreateSavedSearch(streamId);
  const [loaded, HookComponent] = useLoadView(newView, query);

  const _loadNewView = useCallback(() => {
    if (streamId) {
      return loadNewViewForStream(streamId);
    }

    throw new Error('No stream id specified!');
  }, [streamId]);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage loadNewView={_loadNewView} />;
};

export default withParams(withLocation(StreamSearchPage));
