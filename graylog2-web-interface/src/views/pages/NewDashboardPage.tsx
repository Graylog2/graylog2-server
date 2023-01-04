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
import { useMemo } from 'react';

import useLocation from 'routing/useLocation';
import Spinner from 'components/common/Spinner';
import viewTransformer from 'views/logic/views/ViewTransformer';
import View from 'views/logic/views/View';
import { IfPermitted } from 'components/common';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import useQuery from 'routing/useQuery';
import ViewGenerator from 'views/logic/views/ViewGenerator';
import useLoadView from 'views/pages/useLoadView';

import SearchPage from './SearchPage';

type LocationState = {
  view?: View,
};

const NewDashboardPage = () => {
  const location = useLocation<LocationState>();
  const searchView = location?.state?.view;

  const query = useQuery();
  const loadedView = useMemo(() => (searchView?.search
    ? Promise.resolve(viewTransformer(searchView))
    : ViewGenerator(View.Type.Dashboard, undefined)),
  // This should be run only once upon mount on purpose.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  []);
  useLoadView(loadedView);
  const [loaded, HookComponent] = useProcessHooksForView(loadedView, query);

  if (HookComponent) {
    return HookComponent;
  }

  return loaded
    ? <IfPermitted permissions="dashboards:create"><SearchPage /></IfPermitted>
    : <Spinner />;
};

export default NewDashboardPage;
