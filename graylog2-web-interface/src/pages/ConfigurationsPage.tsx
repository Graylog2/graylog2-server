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
import { useMemo } from 'react';
import styled from 'styled-components';
import { Navigate, Routes, Route, useResolvedPath } from 'react-router-dom';
import URI from 'urijs';

import { isPermitted } from 'util/PermissionsMixin';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import { Col, Nav, NavItem } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Icon } from 'components/common';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlAllowListConfig from 'components/configurations/UrlAllowListConfig';
import PermissionsConfig from 'components/configurations/PermissionsConfig';
import PluginsConfig from 'components/configurations/PluginsConfig';
import McpConfig from 'components/configurations/McpConfig';
import 'components/maps/configurations';
import useCurrentUser from 'hooks/useCurrentUser';
import { LinkContainer } from 'components/common/router';
import useLocation from 'routing/useLocation';
import MarkdownConfig from 'components/configurations/MarkdownConfig';

import ConfigurationSection from './configurations/ConfigurationSection';
import type { ConfigurationSectionProps } from './configurations/ConfigurationSection';

import DecoratorsConfig from '../components/configurations/DecoratorsConfig';
import UserConfig from '../components/configurations/UserConfig';

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
  const currentUser = useCurrentUser();

  const configurationSections: Array<{
    name: string;
    hide?: boolean;
    SectionComponent: React.ComponentType<ConfigurationSectionProps | {}>;
    props?: ConfigurationSectionProps;
    showCaret?: boolean;
    catchAll?: boolean;
  }> = useMemo(
    () =>
      [
        {
          name: 'Search',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: SearchesConfig,
            title: 'Search',
          },
        },
        {
          name: 'Message Processors',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: MessageProcessorsConfig,
            title: 'Message Processors',
          },
        },
        {
          name: 'Sidecars',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: SidecarConfig,
            title: 'Sidecars',
          },
        },
        {
          name: 'Events',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: EventsConfig,
            title: 'Events',
          },
        },
        {
          name: 'URL Allowlist',
          hide: !isPermitted(currentUser.permissions, ['urlallowlist:read']),
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: UrlAllowListConfig,
            title: 'URL allowlist',
          },
        },
        {
          name: 'Decorators',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: DecoratorsConfig,
            title: 'Decorators',
          },
        },
        {
          name: 'Permissions',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: PermissionsConfig,
            title: 'Permissions',
          },
        },
        {
          name: 'Users',
          hide: !isPermitted(currentUser.permissions, ['users:edit', 'users:tokencreate']),
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: UserConfig,
            title: 'Users',
          },
        },
        {
          name: 'Markdown',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: MarkdownConfig,
            title: 'Markdown',
          },
        },
        {
          name: 'Plugins',
          SectionComponent: PluginsConfig,
          showCaret: true,
          catchAll: true,
        },
        {
          name: 'MCP',
          SectionComponent: ConfigurationSection,
          props: {
            ConfigurationComponent: McpConfig,
            title: 'MCP',
          },
        },
      ].filter(({ hide }) => !hide),
    [currentUser?.permissions],
  );

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
