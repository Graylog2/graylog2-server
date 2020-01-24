// @flow strict
import React, { useEffect, useState } from 'react';

import UserNotification from 'util/UserNotification';

import { ViewActions } from 'views/stores/ViewStore';
import { SearchActions } from 'views/stores/SearchStore';
import { syncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';

import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import View from 'views/logic/views/View';
import ViewLoader, { processHooks } from 'views/logic/views/ViewLoader';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import withPluginEntities from 'views/logic/withPluginEntities';
import type { ViewHook } from 'views/logic/hooks/ViewHook';

import Spinner from 'components/common/Spinner';
import { ExtendedSearchPage } from 'views/pages';

type URLQuery = { [string]: any }

type Props = {
  executingViewHooks: Array<ViewHook>,
  loadingViewHooks: Array<ViewHook>,
  location: {
    query: { [string]: any },
  },
  params: {
    streamId: string,
  },
  route: {},
  router: {
    getCurrentLocation: () => ({ pathname: string, search: string }),
  },
};

const StreamSearchPage = ({ params: { streamId }, route, router, loadingViewHooks, executingViewHooks, location }: Props) => {
  const [loaded, setLoaded] = useState(false);
  const { query } = location;
  const [hookComponent, setHookComponent] = useState(undefined);

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

  const loadNewView = (currentURLQuery: URLQuery): Promise<?View> => {
    return processHooks(
      ViewActions.create(View.Type.Search, streamId).then(({ view }) => view),
      loadingViewHooks,
      executingViewHooks,
      currentURLQuery,
    ).then(
      () => setLoaded(true),
    ).catch(
      error => UserNotification.error(`Executing search failed with error: ${error}`, 'Could not execute search'),
    );
  };

  const loadViewFromParams = (): Promise<?View> => {
    return loadNewView({ ...query });
  };

  const loadEmptyView = (): Promise<?View> => {
    return loadNewView({}).then(() => {
      const { pathname, search } = router.getCurrentLocation();
      const currentQuery = `${pathname}${search}`;
      syncWithQueryParameters(currentQuery);
    });
  };

  useEffect(() => { loadViewFromParams(); }, [streamId]);

  if (hookComponent) {
    return (<>{hookComponent}</>);
  }

  if (loaded) {
    return (
      <ViewLoaderContext.Provider value={loadView}>
        <NewViewLoaderContext.Provider value={loadEmptyView}>
          <ExtendedSearchPage route={route} />
        </NewViewLoaderContext.Provider>
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
