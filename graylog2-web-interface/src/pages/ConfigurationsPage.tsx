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

import { Col, Nav, NavItem, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { useStore } from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
// import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlWhiteListConfig from 'components/configurations/UrlWhiteListConfig';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IndexSetsDefaultsConfig from 'components/configurations/IndexSetsDefaultsConfig';
import PermissionsConfig from 'components/configurations/PermissionsConfig';
import 'components/maps/configurations';
import type { Store } from 'stores/StoreTypes';
import usePluginEntities from 'hooks/usePluginEntities';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import useCurrentUser from 'hooks/useCurrentUser';

import ConfigletContainer from './configurations/ConfigletContainer';

import DecoratorsConfig from '../components/configurations/DecoratorsConfig';
import UserConfig from '../components/configurations/UserConfig';


const getConfig = (configType, configuration) => configuration?.[configType] ?? null;

const onUpdate = (configType: string) => {
  return (config) => {
    switch (configType) {
      default:
        return ConfigurationsActions.update(configType, config);
    }
  };
};

const ConfigurationsPage = () => {
  const [loaded, setLoaded] = useState(false);
  const pluginSystemConfigs = usePluginEntities('systemConfigurations');
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const currentUser = useCurrentUser();
  const [activeSectionKey, setActiveSectionKey] = useState(1);
  const [activeSubSectionKey, setActiveSubSectionKey] = useState(1);

  useEffect(() => {
    // if (isPermitted(currentUser.permissions, ['urlwhitelist:read'])) {
    //   promises.push(ConfigurationsActions.listWhiteListConfig(ConfigurationType.URL_WHITELIST_CONFIG));
    // }
    const pluginPromises = pluginSystemConfigs
      .map((systemConfig) => ConfigurationsActions.list(systemConfig.configType));

    Promise.allSettled(pluginPromises).then(() => setLoaded(true));
  }, [currentUser.permissions, pluginSystemConfigs]);

  const handleNavSelect = (itemKey) => {
    setActiveSectionKey(itemKey);
    setActiveSubSectionKey(1);
  };

  const handleSubNavSelect = (itemKey) => {
    setActiveSubSectionKey(itemKey);
  };


  const pluginDisplayNames = [
    {
      configType: 'org.graylog.plugins.collector.system.CollectorSystemConfiguration',
      displayName: 'Collectors System',
    },
    {
      configType: 'org.graylog.aws.config.AWSPluginConfiguration',
      displayName: 'AWS',
    },
    {
      configType: 'org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration',
      displayName: 'Threat Intelligence Lookup',
    },
    {
      configType: 'org.graylog.plugins.failure.config.EnterpriseFailureHandlingConfiguration',
      displayName: 'Failure Processing',
    },
    {
      configType: 'org.graylog.plugins.license.violations.TrafficLimitViolationSettings',
      displayName: 'Traffic Limit Violation',
    },
    {
      configType: 'org.graylog.plugins.map.config.GeoIpResolverConfig',
      displayName: 'Geo-Location Processor',
    },
  ];

  const configurationSections = [
    {
      name: 'Search',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Search Configuration" key={key}>
          <SearchesConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Message Processor',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Message Processor Configuration" key={key}>
          <MessageProcessorsConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Sidecar',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Sidecar Configuration" key={key}>
          <SidecarConfig  />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Events',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Events Configuration" key={key}>
          <EventsConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'URL Whitelist',
      shouldRender: true,
      render: (key) => (
        isPermitted(currentUser.permissions, ['urlwhitelist:read']) && (
        <ConfigletContainer title="URL Whitelist Configuration" key={key}>
          <UrlWhiteListConfig />
        </ConfigletContainer>
        )
      ),
    },
    {
      name: 'Decorators',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Decorators Configuration" key={key}>
          <DecoratorsConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Permissions',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="Permissions Configuration" key={key}>
          <PermissionsConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Index Set Defaults',
      shouldRender: true,
      render: (key) => (
        <HideOnCloud key={key}>
          <ConfigletContainer title="Index Set Default Configuration">
            <IndexSetsDefaultsConfig  />
          </ConfigletContainer>
        </HideOnCloud>
      ),
    },
    {
      name: 'Users',
      shouldRender: true,
      render: (key) => (
        <ConfigletContainer title="User Configuration" key={key}>
          <UserConfig />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Plugins',
      shouldRender: pluginSystemConfigs.length > 0,
      render: (key) => (
        <Row key={key}>
          <Col md={3}>
            <Nav bsStyle="pills" stacked activeKey={activeSubSectionKey} onSelect={handleSubNavSelect}>
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
                (index + 1 === activeSubSectionKey) && (
                <ConfigletContainer title={configType} key={`plugin-section-${configType}`}>
                  <SystemConfigComponent key={`system-configuration-${configType}`}
                                         config={getConfig(configType, configuration) ?? undefined}
                                         updateConfig={onUpdate(configType)} />
                </ConfigletContainer>
                )
              ))}
          </Col>
        </Row>
      ),
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
        {loaded ? (
          <>
            <Col md={2}>
              <Nav bsStyle="pills" stacked activeKey={activeSectionKey} onSelect={handleNavSelect}>
                {configurationSections.map((section, index) => (
                  section.shouldRender && (
                  <NavItem key={`nav-${section.name}`} eventKey={index + 1} title={section.name}>
                    {section.name}
                  </NavItem>
                  )
                ))}
              </Nav>
            </Col>

            <Col md={10}>
              {configurationSections.map((section) => (
                isSectionActive(section.name) && section.shouldRender && section.render(`section-${section.name}`)
              ))}
            </Col>

          </>
        ) : (
          <Col md={12}>
            <Spinner text="Loading Configuration Panel..." />
          </Col>
        )}
      </ConfigletRow>
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
