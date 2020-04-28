// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import Spinner from 'components/common/Spinner';

import type { ViewHook } from 'views/logic/hooks/ViewHook';
import { processHooks } from 'views/logic/views/ViewLoader';
import withPluginEntities from 'views/logic/withPluginEntities';
import viewTransformer from 'views/logic/views/ViewTransformer';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';
import { ExtendedSearchPage } from 'views/pages';
import { IfPermitted } from 'components/common';

type Props = {
  route: {},
  location: {
    state?: {
      view?: View | ViewJson,
    },
    query: { [string]: any },
  };
  loadingViewHooks: Array<ViewHook>,
  executingViewHooks: Array<ViewHook>,
};
const NewDashboardPage = ({ route, location, loadingViewHooks, executingViewHooks }: Props) => {
  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    let mounted = true;
    const { state = {} } = location;
    const { view: searchView } = state;
    if (searchView && searchView.search) {
      const { query } = location;
      /* $FlowFixMe the searchView.search is guard enough and instanceof does not work here */
      const dashboardView = viewTransformer(searchView);
      const loadPromise = ViewActions.load(dashboardView, true).then(() => dashboardView);
      processHooks(
        loadPromise,
        loadingViewHooks,
        executingViewHooks,
        query,
        () => {
          setHookComponent(undefined);
          setLoaded(true);
        },
      ).catch((e) => setHookComponent(e));
    } else {
      ViewActions.create(View.Type.Dashboard).then(() => mounted && setLoaded(true));
    }
    return () => { mounted = false; };
  }, []);

  if (hookComponent) {
    return (<>{hookComponent}</>);
  }

  return loaded
    ? <IfPermitted permissions="dashboards:create"><ExtendedSearchPage route={route} /></IfPermitted>
    : <Spinner />;
};

NewDashboardPage.propTypes = {
  route: PropTypes.object.isRequired,
};

const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(NewDashboardPage, mapping);
