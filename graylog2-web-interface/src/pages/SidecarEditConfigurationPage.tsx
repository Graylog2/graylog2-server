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
import { useQuery } from '@tanstack/react-query';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import withParams from 'routing/withParams';
import { fetchConfiguration, fetchConfigurationSidecars } from 'hooks/useCollectorConfigurations';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import DocsHelper from 'util/DocsHelper';
import useHistory from 'routing/useHistory';

type SidecarEditConfigurationPageProps = {
  params: any;
};

const SidecarEditConfigurationPage = ({ params }: SidecarEditConfigurationPageProps) => {
  const history = useHistory();
  const { configurationId } = params;

  const { data: configuration, error: configurationError } = useQuery({
    queryKey: ['collector-configurations', 'detail', configurationId],
    queryFn: () => fetchConfiguration(configurationId),
  });

  const { data: configurationSidecars } = useQuery({
    queryKey: ['collector-configurations', 'sidecars', configurationId],
    queryFn: () => fetchConfigurationSidecars(configurationId),
    enabled: !!configuration,
  });

  useEffect(() => {
    if (configurationError && (configurationError as { status?: number }).status === 404) {
      history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION);
    }
  }, [configurationError, history]);

  if (!configuration || !configurationSidecars) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Collector Configuration">
      <SidecarsPageNavigation />
      <PageHeader
        title="Collector Configuration"
        documentationLink={{
          title: 'Sidecar documentation',
          path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
        }}>
        <span>Some words about collector configurations.</span>
      </PageHeader>
      <ConfigurationForm configuration={configuration} configurationSidecars={configurationSidecars} />
    </DocumentTitle>
  );
};

export default withParams(SidecarEditConfigurationPage);
