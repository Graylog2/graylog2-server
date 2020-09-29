// @flow strict
import React, { useMemo } from 'react';
import PropTypes from 'prop-types';

import Spinner from 'components/common/Spinner';
import withPluginEntities from 'views/logic/withPluginEntities';
import viewTransformer from 'views/logic/views/ViewTransformer';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';
import { ExtendedSearchPage } from 'views/pages';
import { IfPermitted } from 'components/common';
import useLoadView from 'views/logic/views/UseLoadView';

type Props = {
  route: {},
  location: {
    state?: {
      view?: View | ViewJson,
    },
    query: { [string]: any },
  },
};

const NewDashboardPage = ({ route, location }: Props) => {
  const loadedView = useMemo(() => {
    const { state = {} } = location;
    const { view: searchView } = state;

    if (searchView?.search) {
      /* $FlowFixMe the searchView.search is guard enough and instanceof does not work here */
      const dashboardView = viewTransformer(searchView);

      return ViewActions.load(dashboardView, true).then(() => dashboardView);
    }

    return ViewActions.create(View.Type.Dashboard).then(({ view }) => view);
  }, []);

  const [loaded, HookComponent] = useLoadView(loadedView, location.query);

  if (HookComponent) {
    return <HookComponent />;
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
