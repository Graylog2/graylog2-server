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
import { PluginStore } from 'graylog-web-plugin/plugin';

import TeaserSearch from 'components/security/teaser/TeaserSearch';

import viewJson from './sample-dashboards/overview_view.json';
import searchJson from './sample-dashboards/overview_search.json';
import resultJson from './sample-dashboards/overview_results.json';

const ClusterManagementOverview = () => {
  const datanodePlugin = PluginStore.exports('datanode');
  const ClusterManagementSearch = datanodePlugin[0]?.ClusterManagementSearch;

  if (ClusterManagementSearch) {
    return <ClusterManagementSearch />;
  }

  return <TeaserSearch viewJson={viewJson} searchJson={searchJson} searchJobResult={resultJson} hotspots={[]} />;
};

export default ClusterManagementOverview;
