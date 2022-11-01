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
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { List, Map } from 'immutable';

import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';

// eslint-disable-next-line no-alert
const defaultConfirm = async () => window.confirm('Do you really want to delete this dashboard page?');

const ConfirmDeletingDashboardPage = async (dashboardId: string, activeQueryId: string, widgetIds: Map<string, List<string>>) => {
  const _widgetIds = widgetIds.map((ids) => ids.toArray()).toObject();
  const deletingDashboardPageHooks = PluginStore.exports('views.hooks.confirmDeletingDashboardPage');
  const result = await iterateConfirmationHooks([...deletingDashboardPageHooks, defaultConfirm], dashboardId, activeQueryId, _widgetIds);

  return result;
};

export default ConfirmDeletingDashboardPage;
