// @flow strict
import React, { useEffect, useState } from 'react';

import UserNotification from 'util/UserNotification';

import ViewLoader, { processHooks } from 'views/logic/views/ViewLoader';
import { QueryFiltersActions } from 'views/stores/QueryFiltersStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import withPluginEntities from 'views/logic/withPluginEntities';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Spinner from 'components/common/Spinner';
import { ExtendedSearchPage } from 'views/pages';
import { SearchActions } from 'views/stores/SearchStore';


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
  const [hookComponent, setHookComponent] = useState(undefined);

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

  const loadView = (viewId: string): Promise<?View> => {
    return ViewLoader(
      viewId,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => {
        setHookComponent(undefined);
      },
      (e) => {
        if (e instanceof Error) {
          throw e;
        }
        setHookComponent(e);
      },
    ).then((view) => {
      setLoaded(true);
      return view;
    }).then(() => {
      SearchActions.executeWithCurrentState();
    }).catch(e => e);
  };

  if (hookComponent) {
    return (<>{hookComponent}</>);
  }

  if (loaded) {
    return (
      <ViewLoaderContext.Provider value={loadView}>
        <ExtendedSearchPage route={route} />
      </ViewLoaderContext.Provider>
    );
  }
  return <Spinner />;
};

const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(StreamSearchPage, mapping);
