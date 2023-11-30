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
import viewJson from 'components/security/teaser/sample-dashboards/overview_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/overview_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/overview_results.json';

const hotspots = [
  {
    positionX: '50px',
    positionY: '50%',
    description: 'Get a summary of the anomalies that are running, how many have been detected, and how that compares to the previous time period.',
  },
  {
    positionX: '250px',
    positionY: '25%',
    description: 'Confidence intervals tell you how far off the normal the behavior is.',
  },
  {
    positionX: '600px',
    positionY: '80%',
    description: 'There are detectors for different types of anomalies – quickly see which ones are generating alerts.',
  },
];
const SecurityOverview = () => (
  <DocumentTitle title="Overview">
    <TeaserSearch viewJson={viewJson} searchJson={searchJson} searchJobResult={resultJson} hotspots={hotspots} />
  </DocumentTitle>
);

export default SecurityOverview;
