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
import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import useHistory from 'routing/useHistory';
import { DocumentTitle, PageHeader } from 'components/common';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';
import ClusterConfigurationNodes from 'components/cluster-configuration/ClusterConfigurationNodes';
import usePermissions from 'hooks/usePermissions';
import Routes from 'routing/Routes';

const ClusterConfigurationPage = () => {
  const { t } = useTranslation('system');
  const { isPermitted } = usePermissions();
  const { push } = useHistory();

  useEffect(() => {
    if (!isPermitted('clusterconfiguration:read')) {
      push(Routes.NOTFOUND);
    }
  }, [isPermitted, push]);

  return (
    <DocumentTitle title={t('cluster_configuration_page.title')}>
      <ClusterConfigurationPageNavigation />
      <div>
        <PageHeader title={t('cluster_configuration_page.title')}>
          <span>{t('cluster_configuration_page.description')}</span>
        </PageHeader>
        <HideOnCloud>
          <IndexerClusterHealth minimal />
        </HideOnCloud>
        <ClusterConfigurationNodes />
      </div>
    </DocumentTitle>
  );
};

export default ClusterConfigurationPage;
