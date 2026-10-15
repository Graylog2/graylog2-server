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
import { Navigate, Routes, Route, useResolvedPath } from 'react-router-dom';
import URI from 'urijs';

import type { CoreSystemConfiguration } from 'views/types';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import { Col, NavItem } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Icon, LinkContainer, Spinner } from 'components/common';
import PluginsConfig from 'components/configurations/PluginsConfig';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import 'components/maps/configurations';
import useLocation from 'routing/useLocation';
import AppRoutes from 'routing/Routes';
import usePluginEntities from 'hooks/usePluginEntities';
import usePermissions from 'hooks/usePermissions';
import useRuntimeTunables from 'components/configurations/runtime-tunables/useRuntimeTunables';
import buildSections from 'components/configurations/runtime-tunables/buildSections';

const SubNavIconClosed = styled(Icon)`
  margin-left: 5px;
  vertical-align: middle;
`;

const SubNavIconOpen = styled(Icon)`
  margin-left: 5px;
`;

type SectionLinkProps = {
  name: string;
  showCaret: boolean;
};

const SectionLink = ({ name, showCaret }: SectionLinkProps) => {
  const absolutePath = useResolvedPath(name);
  const location = useLocation();

  const isActive =
    URI(location.pathname).equals(absolutePath.pathname) || location.pathname.startsWith(absolutePath.pathname);

  return (
    <SectionNavItem className={isActive ? 'active' : undefined}>
      <LinkContainer key={`nav-${name}`} to={name}>
        <NavItem title={name} withMinHeight={false}>
          {name}
          {showCaret &&
            (isActive ? <SubNavIconClosed name="arrow_right" /> : <SubNavIconOpen name="arrow_drop_down" />)}
        </NavItem>
      </LinkContainer>
    </SectionNavItem>
  );
};

const pluginsSection: CoreSystemConfiguration = {
  name: 'Plugins',
  SectionComponent: PluginsConfig,
  showCaret: true,
  catchAll: true,
};

const ConfigurationsPage = () => {
  const coreSystemConfigurations = usePluginEntities('coreSystemConfigurations');
  const pluginSystemConfigurations = usePluginEntities('systemConfigurations');
  const { features, isLoading } = useRuntimeTunables();
  const { isPermitted } = usePermissions();

  const configurationSections = [
    ...buildSections({ features, coreConfigurations: coreSystemConfigurations, isPermitted }),
    ...(pluginSystemConfigurations.length > 0 ? [pluginsSection] : []),
  ];

  return (
    <DocumentTitle title="Configurations">
      <PageHeader title="Configurations">
        <span>You can configure system settings for different sub systems on this page.</span>
      </PageHeader>

      {isLoading ? (
        <Spinner />
      ) : (
        <ConfigletRow className="content">
          <Col md={2}>
            <SectionNav>
              {configurationSections.map(({ name, showCaret }) => (
                <SectionLink key={`nav-${name}`} name={name} showCaret={showCaret} />
              ))}
            </SectionNav>
          </Col>

          <Routes>
            <Route path="/" element={<Navigate to={configurationSections[0].name} replace />} />
            <Route
              path="*"
              element={<Navigate to={AppRoutes.SYSTEM.configurationsSection(configurationSections[0].name)} replace />}
            />
            {configurationSections.flatMap(({ catchAll, name, props = {}, SectionComponent }) => (
              <Route
                path={catchAll ? `${name}/*` : name}
                key={name}
                element={<SectionComponent {...props} key={name} />}
              />
            ))}
          </Routes>
        </ConfigletRow>
      )}
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
