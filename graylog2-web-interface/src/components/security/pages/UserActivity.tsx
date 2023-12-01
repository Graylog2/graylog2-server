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
import viewJson from 'components/security/teaser/sample-dashboards/user_activity_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/user_activity_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/user_activity_results.json';

const hotspots = [
  {
    positionX: '67%',
    positionY: '260px',
    description: 'Quickly see if you have concerning trends in failed logons.',
  },
  {
    positionX: '40%',
    positionY: '500px',
    description: 'Logon successes and failures by user can help you spot problem accounts and unusual activity.',
  },
  {
    positionX: '70%',
    positionY: '500px',
    description: 'These are the top 15 user accounts generating high alerts!',
  },
  {
    positionX: '40%',
    positionY: '940px',
    description: 'What are the most common identity and access control changes made in your environment?',
  },
  {
    positionX: '50%',
    positionY: '1300px',
    description: 'Know the target of the most frequent access control changes.',
  },
];

const UserActivity = () => (
  <DocumentTitle title="User activity">
    <TeaserSearch viewJson={viewJson} searchJson={searchJson} searchJobResult={resultJson} hotspots={hotspots} />
  </DocumentTitle>
);

export default UserActivity;
