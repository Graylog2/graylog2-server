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
import viewJson from 'components/security/teaser/sample-dashboards/anomalies_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/anomalies_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/anomalies_results.json';

const hotspots = [
  {
    positionX: '50%',
    positionY: '110px',
    description: 'Get a summary of the anomalies that are running, how many have been detected, and how that compares to the previous time period.',
  },
  {
    positionX: '60%',
    positionY: '550px',
    description: 'Confidence intervals tell you how far off the normal the behavior is.',
  },
  {
    positionX: '40%',
    positionY: '910px',
    description: 'There are detectors for different types of anomalies – quickly see which ones are generating alerts.',
  },
  {
    positionX: '70%',
    positionY: '910px',
    description: 'Identify user accounts that have unusual-for-them behaviors for logons or security events.',
  },
];
const Anomalies = () => (
  <DocumentTitle title="Anomalies">
    <TeaserSearch viewJson={viewJson} searchJson={searchJson} searchJobResult={resultJson} hotspots={hotspots} />
  </DocumentTitle>
);

export default Anomalies;
