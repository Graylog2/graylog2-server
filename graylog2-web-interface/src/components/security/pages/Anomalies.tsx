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
import viewJson from 'components/security/teaser/sample-dashboards/anomalies_view.json';
import searchJson from 'components/security/teaser/sample-dashboards/anomalies_search.json';
import resultJson from 'components/security/teaser/sample-dashboards/anomalies_results.json';
import type { SearchJson } from 'views/logic/search/Search';

const hotspots = [
  {
    positionX: '50%',
    positionY: '110px',
    descriptionKey: 'security_teaser.anomalies.hotspot_0',
  },
  {
    positionX: '60%',
    positionY: '550px',
    descriptionKey: 'security_teaser.anomalies.hotspot_1',
  },
  {
    positionX: '40%',
    positionY: '910px',
    descriptionKey: 'security_teaser.anomalies.hotspot_2',
  },
  {
    positionX: '70%',
    positionY: '910px',
    descriptionKey: 'security_teaser.anomalies.hotspot_3',
  },
];
const Anomalies = () => {
  const { t } = useTranslation('users');

  return (
    <DocumentTitle title={t('security_teaser.anomalies.title')}>
      <TeaserSearch
        viewJson={viewJson}
        searchJson={searchJson as SearchJson}
        searchJobResult={resultJson}
        hotspots={hotspots}
      />
    </DocumentTitle>
  );
};

export default Anomalies;
