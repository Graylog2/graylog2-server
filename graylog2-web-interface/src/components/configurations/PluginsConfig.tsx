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
import { useEffect, useState } from 'react';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import usePluginEntities from 'hooks/usePluginEntities';
import { getConfig } from 'components/configurations/helpers';
import { PluginConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Col, Nav, NavItem, Row } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import ConfigletContainer from 'pages/configurations/ConfigletContainer';

const PluginsConfig = () => {
  const [activeSectionKey, setActiveSectionKey] = useState(1);
  const [isLoaded, setIsLoaded] = useState(false);
  const pluginSystemConfigs = usePluginEntities('systemConfigurations');
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  useEffect(() => {
    const pluginPromises = pluginSystemConfigs
      .map((systemConfig) => ConfigurationsActions.list(systemConfig.configType));

    Promise.allSettled(pluginPromises).then(() => {
      setIsLoaded(true);
    });
  }, [configuration, pluginSystemConfigs]);

  const pluginDisplayNames = [
    {
      configType: PluginConfigurationType.COLLECTORS_SYSTEM,
      displayName: 'Collectors System',
    },
    {
      configType: PluginConfigurationType.AWS,
      displayName: 'AWS',
    },
    {
      configType: PluginConfigurationType.THREAT_INTEL,
      displayName: 'Threat Intelligence Lookup',
    },
    {
      configType: PluginConfigurationType.FAILURE_PROCESSING,
      displayName: 'Failure Processing',
    },
    {
      configType: PluginConfigurationType.TRAFFIC_LIMIT_VIOLATION,
      displayName: 'Traffic Limit Violation',
    },
    {
      configType: PluginConfigurationType.GEO_LOCATION,
      displayName: 'Geo-Location Processor',
    },
  ];

  const onUpdate = (configType: string) => {
    return (config) => {
      return ConfigurationsActions.update(configType, config);
    };
  };

  if (!isLoaded || !pluginSystemConfigs) { return <Spinner />; }

  return (

    <Row>
      <Col md={3}>
        <Nav bsStyle="pills"
             stacked
             activeKey={activeSectionKey}
             onSelect={setActiveSectionKey}>
          {pluginSystemConfigs.map(({ configType }, index) => {
            const { displayName } = pluginDisplayNames.find((entry) => entry.configType === configType);

            return (
              <NavItem key={`plugin-nav-${configType}`} eventKey={index + 1} title={displayName}>
                {displayName}
              </NavItem>
            );
          })}
        </Nav>
      </Col>
      <Col md={9}>
        {pluginSystemConfigs
          .map(({ component: SystemConfigComponent, configType }, index) => (
            (index + 1 === activeSectionKey) && (
            <ConfigletContainer title={configType} key={`plugin-section-${configType}`}>
              <SystemConfigComponent key={`system-configuration-${configType}`}
                                     config={getConfig(configType, configuration) ?? undefined}
                                     updateConfig={onUpdate(configType)} />
            </ConfigletContainer>
            )
          ))}
      </Col>
    </Row>
  );
};

export default PluginsConfig;
