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
import React, { useState, useEffect } from 'react';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import withParams from 'routing/withParams';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';
import type { Configuration, ConfigurationSidecarsResponse } from 'components/sidecars/types';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import DocsHelper from 'util/DocsHelper';
import useHistory from 'routing/useHistory';

type SidecarEditConfigurationPageProps = {
  params: any;
};

const SidecarEditConfigurationPage = ({ params }: SidecarEditConfigurationPageProps) => {
  const [configuration, setConfiguration] = useState<Configuration>(null);
  const [configurationSidecars, setConfigurationSidecars] = useState<ConfigurationSidecarsResponse>(null);
  const history = useHistory();

  useEffect(() => {
    const _reloadConfiguration = () => {
      const { configurationId } = params;

      CollectorConfigurationsActions.getConfiguration(configurationId).then(
        (_configuration) => {
          setConfiguration(_configuration);

          CollectorConfigurationsActions.getConfigurationSidecars(configurationId)
            .then((_configurationSidecars) => setConfigurationSidecars(_configurationSidecars));
        },
        (error) => {
          if (error.status === 404) {
            history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION);
          }
        },
      );
    };

    _reloadConfiguration();
  }, [history, params]);

  const _isLoading = () => !configuration || !configurationSidecars;

  if (_isLoading()) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Collector Configuration">
      <SidecarsPageNavigation />
      <PageHeader title="Collector Configuration"
                  documentationLink={{
                    title: 'Sidecar documentation',
                    path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                  }}>
        <span>
          Some words about collector configurations.
        </span>
      </PageHeader>
      <ConfigurationForm configuration={configuration}
                         configurationSidecars={configurationSidecars} />
    </DocumentTitle>
  );
};

export default withParams(SidecarEditConfigurationPage);
