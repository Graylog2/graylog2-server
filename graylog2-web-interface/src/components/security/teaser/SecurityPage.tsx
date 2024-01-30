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
import AppRoutes, { SECURITY_PATH } from 'routing/Routes';
import {
  Overview,
  UserActivity,
  HostActivity,
  NetworkActivity,
  Anomalies,
} from 'components/security/pages';

const subRoutes = [
  { path: AppRoutes.SECURITY.OVERVIEW, element: <Overview /> },
  { path: AppRoutes.SECURITY.USER_ACTIVITY, element: <UserActivity /> },
  { path: AppRoutes.SECURITY.HOST_ACTIVITY, element: <HostActivity /> },
  { path: AppRoutes.SECURITY.NETWORK_ACTIVITY, element: <NetworkActivity /> },
  { path: AppRoutes.SECURITY.ANOMALIES, element: <Anomalies /> },
].map((route) => ({ ...route, path: route.path.slice(SECURITY_PATH.length) }));

const SecurityPage = () => (
  <TeaserPageLayout>
    <Routes>
      {subRoutes.map(({ path, element }) => (
        <Route key={path} path={path} element={element} />
      ))}
    </Routes>
  </TeaserPageLayout>
);

export default SecurityPage;
