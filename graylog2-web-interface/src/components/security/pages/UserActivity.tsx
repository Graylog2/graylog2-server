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
import { useTranslation } from 'react-i18next';

import { DocumentTitle } from 'components/common';
import TeaserSearch from 'components/security/teaser/TeaserSearch';
import viewJson from 'components/security/teaser/sample-dashboards/user_activity_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/user_activity_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/user_activity_results.json';
import type { SearchJson } from 'views/logic/search/Search';

const hotspots = [
  {
    positionX: '67%',
    positionY: '260px',
    descriptionKey: 'security_teaser.user_activity.hotspot_0',
  },
  {
    positionX: '40%',
    positionY: '500px',
    descriptionKey: 'security_teaser.user_activity.hotspot_1',
  },
  {
    positionX: '70%',
    positionY: '500px',
    descriptionKey: 'security_teaser.user_activity.hotspot_2',
  },
  {
    positionX: '40%',
    positionY: '940px',
    descriptionKey: 'security_teaser.user_activity.hotspot_3',
  },
  {
    positionX: '50%',
    positionY: '1300px',
    descriptionKey: 'security_teaser.user_activity.hotspot_4',
  },
];

const UserActivity = () => {
  const { t } = useTranslation('users');

  return (
    <DocumentTitle title={t('security_teaser.user_activity.title')}>
      <TeaserSearch
        viewJson={viewJson}
        searchJson={searchJson as SearchJson}
        searchJobResult={resultJson}
        hotspots={hotspots}
      />
    </DocumentTitle>
  );
};

export default UserActivity;
