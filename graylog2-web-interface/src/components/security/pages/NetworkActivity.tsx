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

import { DocumentTitle } from 'components/common';
import TeaserSearch from 'components/security/teaser/TeaserSearch';
import viewJson from 'components/security/teaser/sample-dashboards/network_activity_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/network_activity_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/network_activity_results.json';

const hotspots = [
  {
    positionX: '50%',
    positionY: '230px',
    description: 'Instantly see spikes in data flows across your network.',
  },
  {
    positionX: '70%',
    positionY: '600px',
    description: 'Quickly see where the data is coming from and where it is going.',
  },
  {
    positionX: '70%',
    positionY: '1030px',
    description: 'Identify which users are sending the most amount of data across the network.',
  },
  {
    positionX: '70%',
    positionY: '1450px',
    description: 'Determine if there is an unusual pattern in DNS query results like a particular error code suddenly showing up in the Top 15 list. ',
  },
  {
    positionX: '40%',
    positionY: '1850px',
    description: 'The top 15 platforms generating DNS requests by event source.',
  },
];

const NetworkActivity = () => (
  <DocumentTitle title="Network activity">
    <TeaserSearch viewJson={viewJson} searchJson={searchJson} searchJobResult={resultJson} hotspots={hotspots} />
  </DocumentTitle>
);

export default NetworkActivity;
