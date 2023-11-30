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
// import viewJson from 'components/security/teaser/sample-dashboards/host_activity_view.json';
// import searchJson from 'components/security/teaser/sample-dashboards/host_activity_search.json';
// import resultJson from 'components/security/teaser/sample-dashboards/host_activity_results.json';

const hotspots = [
  {
    positionX: '50px',
    positionY: '50%',
    description: 'Get a top-level view of your high, medium, and low alert counts with trending information to know if this is a normal day at the office . . . or not.',
  },
  {
    positionX: '50px',
    positionY: '50%',
    description: 'Immediately identify which hosts are generating the high alerts, to shrink MTTR.',
  },
  {
    positionX: '50px',
    positionY: '50%',
    description: 'See which tech is generating the most log-ons.',
  },
  {
    positionX: '50px',
    positionY: '50%',
    description: 'Initiate an investigation when identity and access control changes come from unexpected machines.',
  },
  {
    positionX: '50px',
    positionY: '50%',
    description: 'Message counts by event source give you a daily view of regular patterns in data, spikes outside the norm will trigger investigations as to why.',
  },
];

const SecurityHostActivity = () => (
  <DocumentTitle title="Host activity">
    <TeaserSearch viewJson={{}} searchJson={{}} searchJobResult={{}} hotspots={hotspots} />
  </DocumentTitle>
);

export default SecurityHostActivity;
