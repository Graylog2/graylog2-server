// @flow strict
import * as React from 'react';
import { useCallback } from 'react';

import Spinner from 'components/common/Spinner';
import withParams from 'routing/withParams';
import useLoadView from 'views/logic/views/UseLoadView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';

import SearchPage from './SearchPage';

import { loadNewViewForStream } from '../logic/views/Actions';

type Props = {
  location: Location,
  params: {
    streamId: string,
  },
  route: {},
};

const StreamSearchPage = ({ params: { streamId }, route, location: { query } }: Props) => {
  const newView = useCreateSavedSearch(streamId);
  const [loaded, HookComponent] = useLoadView(newView, query);

  const _loadNewView = useCallback(() => loadNewViewForStream(streamId), [streamId]);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage route={route} loadNewView={_loadNewView} />;
};

export default withParams(withLocation(StreamSearchPage));
