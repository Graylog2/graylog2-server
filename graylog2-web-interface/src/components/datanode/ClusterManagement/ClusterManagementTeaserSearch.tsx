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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import TeaserSearch from 'components/security/teaser/TeaserSearch';
import { Alert } from 'components/bootstrap';
import type { SearchJson } from 'views/logic/search/Search';

import viewJson from './sample-dashboards/overview_view.json';
import searchJson from './sample-dashboards/overview_search.json';
import resultJson from './sample-dashboards/overview_results.json';

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const ClusterManagementTeaserSearch = () => {
  const { t } = useTranslation('datanode');
  const hotspots = [
    {
      positionX: '50%',
      positionY: '120px',
      description: t('cluster_management.teaser_hotspot'),
    },
  ];

  return (
    <>
      <StyledAlert bsStyle="warning">{t('cluster_management.teaser_license_warning')}</StyledAlert>
      <TeaserSearch
        viewJson={viewJson}
        searchJson={searchJson as SearchJson}
        searchJobResult={resultJson}
        hotspots={hotspots}
      />
    </>
  );
};

export default ClusterManagementTeaserSearch;
