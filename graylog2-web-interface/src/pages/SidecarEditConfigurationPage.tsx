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

const SidecarEditConfigurationPage = ({
  params,
}: SidecarEditConfigurationPageProps) => {
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
