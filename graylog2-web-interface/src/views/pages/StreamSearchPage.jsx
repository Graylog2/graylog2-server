// @flow strict
import React, { useEffect, useState } from 'react';

import UserNotification from 'util/UserNotification';

import { processHooks } from 'views/logic/views/ViewLoader';
import { QueryFiltersActions } from 'views/stores/QueryFiltersStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import withPluginEntities from 'views/logic/withPluginEntities';
import type { ViewHook } from 'views/logic/hooks/ViewHook';

import Spinner from 'components/common/Spinner';
import { ExtendedSearchPage } from 'views/pages';


type Props = {
  params: {
    streamId: string,
  },
  location: {
    query: { [string]: any },
  },
  route: {},
  loadingViewHooks: Array<ViewHook>,
  executingViewHooks: Array<ViewHook>,
};

const StreamSearchPage = ({ params: { streamId }, route, loadingViewHooks, executingViewHooks, location }: Props) => {
  const [loaded, setLoaded] = useState(false);
  const { query } = location;
  useEffect(() => {
    processHooks(
      ViewActions.create(View.Type.Search)
        .then(({ view, activeQuery }) => {
          return QueryFiltersActions.streams(activeQuery, [streamId]).then(() => view);
        }),
      loadingViewHooks,
      executingViewHooks,
      query,
    ).then(
      () => setLoaded(true),
    ).catch(
      (error) => { UserNotification.error(`Executing search failed with error: ${error}`, 'Could not execute search'); },
    );
  }, []);

  return loaded ? <ExtendedSearchPage route={route} /> : <Spinner />;
};

const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(StreamSearchPage, mapping);
