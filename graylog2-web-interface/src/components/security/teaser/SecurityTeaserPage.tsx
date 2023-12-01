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

import { Routes, Route } from 'react-router-dom';
import React from 'react';

import TeaserPageLayout from 'components/security/teaser/TeaserPageLayout';
import AppRoutes from 'routing/Routes';
import {
  SecurityOverview,
  SecurityUserActivity,
  SecurityHostActivity,
  SecurityNetworkActivity, SecurityAnomalies,
} from 'components/security/pages';

const subRoutes = [
  { route: AppRoutes.SECURITY.overview(''), component: <SecurityOverview /> },
  { route: AppRoutes.SECURITY.userActivity(''), component: <SecurityUserActivity /> },
  { route: AppRoutes.SECURITY.hostActivity(''), component: <SecurityHostActivity /> },
  { route: AppRoutes.SECURITY.networkActivity(''), component: <SecurityNetworkActivity /> },
  { route: AppRoutes.SECURITY.anomalies(''), component: <SecurityAnomalies /> },
];

const SecurityTeaserPage = () => (
  <TeaserPageLayout>
    <Routes>
      {subRoutes.map(({ route, component }) => (
        <Route key={route} path={route} element={component} />
      ))}
    </Routes>
  </TeaserPageLayout>
);

export default SecurityTeaserPage;
