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
import { useMemo } from 'react';

import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';
import Spinner from 'components/common/Spinner';
import viewTransformer from 'views/logic/views/ViewTransformer';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import { IfPermitted } from 'components/common';
import useLoadView from 'views/logic/views/UseLoadView';

import SearchPage from './SearchPage';

type Props = {
  location: Location & {
    state?: {
      view?: View,
    },
  },
};

const NewDashboardPage = ({ location }: Props) => {
  const { state = {} } = location;
  const { view: searchView } = state;
  const loadedView = useMemo(() => {
    if (searchView?.search) {
      const dashboardView = viewTransformer(searchView);

      return ViewActions.load(dashboardView, true).then(() => dashboardView);
    }

    return ViewActions.create(View.Type.Dashboard).then(({ view }) => view);
  }, [searchView]);

  const [loaded, HookComponent] = useLoadView(loadedView, location.query);

  if (HookComponent) {
    return HookComponent;
  }

  return loaded
    ? <IfPermitted permissions="dashboards:create"><SearchPage /></IfPermitted>
    : <Spinner />;
};

NewDashboardPage.propTypes = {};
export default withLocation(NewDashboardPage);
