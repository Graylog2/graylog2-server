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

import AppConfig from 'util/AppConfig';
import { isPermitted } from 'util/PermissionsMixin';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import { Col, Nav, NavItem } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Icon } from 'components/common';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlWhiteListConfig from 'components/configurations/UrlWhiteListConfig';
import IndexSetsDefaultsConfig from 'components/configurations/IndexSetsDefaultsConfig';
import PermissionsConfig from 'components/configurations/PermissionsConfig';
import PluginsConfig from 'components/configurations/PluginsConfig';
import 'components/maps/configurations';
import useCurrentUser from 'hooks/useCurrentUser';
import { LinkContainer } from 'components/common/router';
import useLocation from 'routing/useLocation';
import KeyboardShortcutsSection from 'pages/configurations/KeyboardShortcutsSection';

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
  name: string,
  showCaret: boolean,
}

const SectionLink = ({ name, showCaret }: SectionLinkProps) => {
  const absolutePath = useResolvedPath(name);
  const location = useLocation();

  const isActive = URI(location.pathname).equals(absolutePath.pathname)
    || location.pathname.startsWith(absolutePath.pathname);

  return (
    <LinkContainer key={`nav-${name}`} to={name}>
      <NavItem title={name} active={isActive}>
        {name}
        {showCaret && (isActive ? <SubNavIconClosed name="caret-right" /> : <SubNavIconOpen name="caret-down" />)}
      </NavItem>
    </LinkContainer>
  );
};

const ConfigurationsPage = () => {
  const currentUser = useCurrentUser();
  const isCloud = AppConfig.isCloud();

  const configurationSections: Array<{
    name: string,
    hide?: boolean,
    SectionComponent: React.ComponentType<ConfigurationSectionProps | {}>,
    props?: ConfigurationSectionProps,
    showCaret?: boolean,
    catchAll?: boolean,
  }> = useMemo(() => [
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
      name: 'URL Whitelist',
      hide: !isPermitted(currentUser.permissions, ['urlwhitelist:read']),
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UrlWhiteListConfig,
        title: 'URL Whitelist',
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
      name: 'Index Set Defaults',
      hide: isCloud,
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: IndexSetsDefaultsConfig,
        title: 'Index Set Defaults',
      },
    },
    {
      name: 'Users',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UserConfig,
        title: 'Index Set Defaults',
      },
    },
    {
      name: 'Plugins',
      SectionComponent: PluginsConfig,
      showCaret: true,
      catchAll: true,
    },
    {
      name: 'Keyboard Shortcuts',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: KeyboardShortcutsSection,
        title: 'Graylog Keyboard Shortcuts',
      },
    },
  ].filter(({ hide }) => !hide), [currentUser?.permissions, isCloud]);

  return (
    <DocumentTitle title="Configurations">
      <PageHeader title="Configurations">
        <span>
          You can configure system settings for different sub systems on this page.
        </span>
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
            <Route path={catchAll ? `${name}/*` : name}
                   key={name}
                   element={<SectionComponent {...props} key={name} />} />
          ))}
        </Routes>
      </ConfigletRow>
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
