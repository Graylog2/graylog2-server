// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import Spinner from 'components/common/Spinner';

import { processHooks } from 'views/logic/views/ViewLoader';
import withPluginEntities from 'views/logic/withPluginEntities';
import viewTransformer from 'views/logic/views/ViewTransformer';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  route: {},
  location: {
    state: {
      view?: View,
    },
  };
};
const NewDashboardPage = ({ route, location, loadingViewHooks, executingViewHooks }: Props) => {
  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    const { view: searchView } = location.state;
    if (searchView) {
      const query = location;
      const dashboardView = viewTransformer(searchView);
      const loadPromise = ViewActions.load(dashboardView).then(() => dashboardView);
      processHooks(
        loadPromise,
        loadingViewHooks,
        executingViewHooks,
        query,
        () => {
          setHookComponent(undefined);
          setLoaded(true);
        },
      ).catch(e => setHookComponent(e));
    } else {
      ViewActions.create(View.Type.Dashboard).then(() => setLoaded(true));
    }
  }, []);

  if (hookComponent) {
    const HookComponent = hookComponent;
    return (<>{hookComponent}</>);
  }

  return loaded
    ? <ExtendedSearchPage route={route} />
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
