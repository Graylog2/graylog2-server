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

import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Enterprise } from '@graylog/server-api';

import { Spinner } from 'components/common';
import usePluginEntities from 'hooks/usePluginEntities';
import { defaultOnError } from 'util/conditional/onError';

import IndexerFailuresComponent from './IndexerFailuresComponent';

const IndexerSystemOverviewComponent = () => {
  const { t } = useTranslation('indexsets');
  const [loadIndexerFailuresComponent, setLoadIndexerFailuresComponent] = useState(
    <Spinner text={t('indexers.system_overview.looking_for_failures')} />,
  );

  const pluginSystemOverview = usePluginEntities('systemOverview');
  const EnterpriseIndexerFailures = pluginSystemOverview?.[0]?.component;

  useEffect(() => {
    if (EnterpriseIndexerFailures) {
      defaultOnError(
        Enterprise.licenseInfo(),
        t('indexers.system_overview.license_load_failed'),
        t('indexers.system_overview.error_title'),
      ).then((response: any) => {
        setLoadIndexerFailuresComponent(
          response.license_info.license_status === 'installed' ? (
            <EnterpriseIndexerFailures />
          ) : (
            <IndexerFailuresComponent />
          ),
        );
      });
    } else {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLoadIndexerFailuresComponent(<IndexerFailuresComponent />);
    }
  }, [EnterpriseIndexerFailures, setLoadIndexerFailuresComponent, t]);

  return loadIndexerFailuresComponent;
};

export default IndexerSystemOverviewComponent;
