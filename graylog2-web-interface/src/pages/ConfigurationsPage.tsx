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
import { Col, Nav, NavItem } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Icon } from 'components/common';
import PluginsConfig from 'components/configurations/PluginsConfig';
import 'components/maps/configurations';
import { LinkContainer } from 'components/common/router';
import useLocation from 'routing/useLocation';
import usePluginEntities from 'hooks/usePluginEntities';
import usePermissions from 'hooks/usePermissions';

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
    <LinkContainer key={`nav-${name}`} to={name}>
      <NavItem title={name} active={isActive}>
        {name}
        {showCaret && (isActive ? <SubNavIconClosed name="arrow_right" /> : <SubNavIconOpen name="arrow_drop_down" />)}
      </NavItem>
    </LinkContainer>
  );
};

const ConfigurationsPage = () => {
  const coreSystemConfigurations = usePluginEntities('coreSystemConfigurations');
  const { isPermitted } = usePermissions();

  const configurationSections = [
    ...coreSystemConfigurations,
    {
      name: 'Plugins',
      SectionComponent: PluginsConfig,
      showCaret: true,
      catchAll: true,
    } as CoreSystemConfiguration,
  ].filter(({ permissions }) => (permissions ? isPermitted(permissions) : true));

  return (
    <DocumentTitle title="Configurations">
      <PageHeader title="Configurations">
        <span>You can configure system settings for different sub systems on this page.</span>
      </PageHeader>

      <ConfigletRow className="content">
        <Col md={2}>
          <Nav bsStyle="pills" stacked>
            {configurationSections.map(({ name, showCaret }) => (
              <SectionLink key={`nav-${name}`} name={name} showCaret={showCaret} />
            ))}
          </Nav>
        </Col>

        <Routes>
          <Route path="/" element={<Navigate to={configurationSections[0].name} replace />} />
          {configurationSections.flatMap(({ catchAll, name, props = {}, SectionComponent }) => (
            <Route
              path={catchAll ? `${name}/*` : name}
              key={name}
              element={<SectionComponent {...props} key={name} />}
            />
          ))}
        </Routes>
      </ConfigletRow>
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
