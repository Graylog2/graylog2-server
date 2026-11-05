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
import viewJson from 'components/security/teaser/sample-dashboards/overview_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/overview_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/overview_results.json';
import type { SearchJson } from 'views/logic/search/Search';

const hotspots = [
  {
    positionX: '15%',
    positionY: '50px',
    descriptionKey: 'security_teaser.overview.hotspot_0',
  },
  {
    positionX: '50%',
    positionY: '350px',
    descriptionKey: 'security_teaser.overview.hotspot_1',
  },
  {
    positionX: '75%',
    positionY: '580px',
    descriptionKey: 'security_teaser.overview.hotspot_2',
  },
];
const Overview = () => {
  const { t } = useTranslation('users');

  return (
    <DocumentTitle title={t('security_teaser.overview.title')}>
      <TeaserSearch
        viewJson={viewJson}
        searchJson={searchJson as SearchJson}
        searchJobResult={resultJson}
        hotspots={hotspots}
      />
    </DocumentTitle>
  );
};

export default Overview;
