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
import type { Location } from 'react-router-dom';

import useLocation from 'routing/useLocation';
import viewTransformer from 'views/logic/views/ViewTransformer';
import View from 'views/logic/views/View';
import { IfPermitted } from 'components/common';
import ViewGenerator from 'views/logic/views/ViewGenerator';
import useCreateSearch from 'views/hooks/useCreateSearch';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';

import SearchPage from './SearchPage';

type LocationState = Location & { state: {
  view?: View,
} };

const NewDashboardPage = () => {
  const location: LocationState = useLocation();
  const searchView = location?.state?.view;

  const viewPromise = useMemo(() => (searchView?.search
    ? Promise.resolve(UpdateSearchForWidgets(viewTransformer(searchView)))
    : ViewGenerator({ type: View.Type.Dashboard })),
  // This should be run only once upon mount on purpose.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  []);

  const view = useCreateSearch(viewPromise);

  return <IfPermitted permissions="dashboards:create"><SearchPage view={view} isNew /></IfPermitted>;
};

export default NewDashboardPage;
