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
import { useState } from 'react';
import styled from 'styled-components';

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
import DataNodeConfiguration from 'components/configurations/DataNodeConfiguration/DataNodeConfiguration';

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

const ConfigurationsPage = () => {
  const currentUser = useCurrentUser();
  const [activeSectionKey, setActiveSectionKey] = useState(1);
  const isCloud = AppConfig.isCloud();

  const handleNavSelect = (itemKey) => {
    setActiveSectionKey(itemKey);
  };

  const configurationSections: Array<{
    name: string,
    hide?: boolean,
    SectionComponent: React.ComponentType<ConfigurationSectionProps | {}>,
    props: ConfigurationSectionProps | {},
    showCaret?: boolean,
  }> = [
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
      name: 'Data Node',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: DataNodeConfiguration,
        title: 'Certificate Renewal Policy',
      },
    },
    {
      name: 'Plugins',
      SectionComponent: PluginsConfig,
      showCaret: true,
      props: {},
    },
  ];

  const isSectionActive = (configurationSection: string) : boolean => (
    (configurationSections.findIndex((item) => item.name === configurationSection) + 1) === activeSectionKey
  );

  return (
    <DocumentTitle title="Configurations">
      <PageHeader title="Configurations">
        <span>
          You can configure system settings for different sub systems on this page.
        </span>
      </PageHeader>

      <ConfigletRow className="content">
        <Col md={2}>
          <Nav bsStyle="pills" stacked activeKey={activeSectionKey} onSelect={handleNavSelect}>
            {configurationSections.map(({ hide, name, showCaret }, index) => (
              !hide && (
              <NavItem key={`nav-${name}`} eventKey={index + 1} title={name}>
                {name}
                {showCaret && (isSectionActive(name)
                  ? <SubNavIconClosed name="caret-right" />
                  : <SubNavIconOpen name="caret-down" />)}
              </NavItem>
              )
            ))}
          </Nav>
        </Col>

        {configurationSections.map(({ name, hide, props, SectionComponent }) => (
          isSectionActive(name) && !hide && (
            <SectionComponent {...props} key={name} />
          )
        ))}
      </ConfigletRow>
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
