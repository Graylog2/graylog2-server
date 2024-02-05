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
import { Navigate, Routes, Route, useResolvedPath } from 'react-router-dom';
import URI from 'urijs';

import ConfigletContainer from 'pages/configurations/ConfigletContainer';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import usePluginEntities from 'hooks/usePluginEntities';
import { getConfig } from 'components/configurations/helpers';
import { Col, Nav, NavItem } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import { LinkContainer } from 'components/common/router';
import useLocation from 'routing/useLocation';
import type { SelectCallback } from 'components/bootstrap/types';

type PluginSectionLinkProps = {
  configType: string,
  displayName: string,
}

const PluginSectionLink = ({ configType, displayName }: PluginSectionLinkProps) => {
  const absolutePath = useResolvedPath(configType);
  const location = useLocation();

  const isActive = URI(location.pathname).equals(absolutePath.pathname)
    || location.pathname.startsWith(absolutePath.pathname);

  return (
    <LinkContainer key={`plugin-nav-${configType}`} to={configType}>
      <NavItem title={displayName} active={isActive}>
        {displayName}
      </NavItem>
    </LinkContainer>
  );
};

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

  const onUpdate = (configType: string) => (config) => ConfigurationsActions.update(configType, config);

  if (!isLoaded || !pluginSystemConfigs) {
    return <Spinner />;
  }

  return (
    <>
      <Col md={2}>
        <Nav bsStyle="pills"
             stacked
             activeKey={activeSectionKey}
             onSelect={setActiveSectionKey as SelectCallback}>
          {pluginSystemConfigs.map(({ displayName, configType }) => {
            const name = displayName || configType;

            return <PluginSectionLink key={configType} configType={configType} displayName={name} />;
          })}
        </Nav>
      </Col>
      <Col md={8} lg={5}>
        <Routes>
          <Route path="/" element={<Navigate to={pluginSystemConfigs[0].configType} replace />} />
          {pluginSystemConfigs
            .map(({ component: SystemConfigComponent, configType }) => (
              <Route path={configType}
                     key={configType}
                     element={(
                       <ConfigletContainer title={configType} key={`plugin-section-${configType}`}>
                         <SystemConfigComponent key={`system-configuration-${configType}`}
                                                config={getConfig(configType, configuration) ?? undefined}
                                                updateConfig={onUpdate(configType)} />
                       </ConfigletContainer>
              )} />
            ))}
        </Routes>
      </Col>
    </>
  );
};

export default PluginsConfig;
