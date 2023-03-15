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

const SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig';
const MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig';
const SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration';
const EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration';
const INDEX_SETS_DEFAULTS_CONFIG = 'org.graylog2.configuration.IndexSetsDefaultConfiguration';
const URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist';
const PERMISSIONS_CONFIG = 'org.graylog2.users.UserAndTeamsConfig';
const USER_CONFIG = 'org.graylog2.users.UserConfiguration';

const getConfig = (configType, configuration) => configuration?.[configType] ?? null;

const onUpdate = (configType: string) => {
  return (config) => {
    switch (configType) {
      case MESSAGE_PROCESSORS_CONFIG:
        return ConfigurationsActions.updateMessageProcessorsConfig(configType, config);
      case URL_WHITELIST_CONFIG:
        return ConfigurationsActions.updateWhitelist(configType, config);
      case INDEX_SETS_DEFAULTS_CONFIG:
        return ConfigurationsActions.updateIndexSetDefaults(configType, config);
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
    const promises = [
      ConfigurationsActions.list(SEARCHES_CLUSTER_CONFIG),
      ConfigurationsActions.listMessageProcessorsConfig(MESSAGE_PROCESSORS_CONFIG),
      ConfigurationsActions.list(SIDECAR_CONFIG),
      ConfigurationsActions.list(INDEX_SETS_DEFAULTS_CONFIG),
      ConfigurationsActions.list(EVENTS_CONFIG),
      ConfigurationsActions.listPermissionsConfig(PERMISSIONS_CONFIG),
      ConfigurationsActions.listUserConfig(USER_CONFIG),
    ];

    if (isPermitted(currentUser.permissions, ['urlwhitelist:read'])) {
      promises.push(ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG));
    }

    const pluginPromises = pluginSystemConfigs
      .map((systemConfig) => ConfigurationsActions.list(systemConfig.configType));

    Promise.allSettled([...promises, ...pluginPromises]).then(() => setLoaded(true));
  }, [currentUser.permissions, pluginSystemConfigs]);

  const handleNavSelect = (itemKey) => {
    setActiveSectionKey(itemKey);
    setActiveSubSectionKey(1);
  };

  const handleSubNavSelect = (itemKey) => {
    setActiveSubSectionKey(itemKey);
  };

  const searchesConfig = getConfig(SEARCHES_CLUSTER_CONFIG, configuration);
  const messageProcessorsConfig = getConfig(MESSAGE_PROCESSORS_CONFIG, configuration);
  const sidecarConfig = getConfig(SIDECAR_CONFIG, configuration);
  const eventsConfig = getConfig(EVENTS_CONFIG, configuration);
  const urlWhiteListConfig = getConfig(URL_WHITELIST_CONFIG, configuration);
  const indexSetsDefaultsConfig = getConfig(INDEX_SETS_DEFAULTS_CONFIG, configuration);
  const permissionsConfig = getConfig(PERMISSIONS_CONFIG, configuration);
  const userConfig = getConfig(USER_CONFIG, configuration);

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

  // 0
  // :
  // {configType: 'org.graylog.plugins.collector.system.CollectorSystemConfiguration', component: ƒ}
  // 1
  // :
  // {configType: 'org.graylog.aws.config.AWSPluginConfiguration', component: ƒ}
  // 2
  // :
  // {configType: 'org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration', component: ƒ}
  // 3
  // :
  // {configType: 'org.graylog.plugins.failure.config.EnterpriseFailureHandlingConfiguration', permissions: 'indices:failures', component: ƒ}
  // 4
  // :
  // {configType: 'org.graylog.plugins.license.violations.TrafficLimitViolationSettings', permissions: 'clusterconfigentry:read', component: ƒ}
  // 5
  // :
  // {configType: 'org.graylog.plugins.map.config.GeoIpResolverConfig', component: ƒ}
  // length
  // :
  // 6

  const configurationSections = [
    {
      name: 'Search',
      shouldRender: searchesConfig,
      render: (key) => (
        <ConfigletContainer title="Search Configuration" key={key}>
          <SearchesConfig config={searchesConfig}
                          updateConfig={onUpdate(SEARCHES_CLUSTER_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Message Processor',
      shouldRender: messageProcessorsConfig,
      render: (key) => (
        <ConfigletContainer title="Message Processor Configuration" key={key}>
          <MessageProcessorsConfig config={messageProcessorsConfig}
                                   updateConfig={onUpdate(MESSAGE_PROCESSORS_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Sidecar',
      shouldRender: sidecarConfig,
      render: (key) => (
        <ConfigletContainer title="Sidecar Configuration" key={key}>
          <SidecarConfig config={sidecarConfig}
                         updateConfig={onUpdate(SIDECAR_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Events',
      shouldRender: eventsConfig,
      render: (key) => (
        <ConfigletContainer title="Events Configuration" key={key}>
          <EventsConfig config={eventsConfig}
                        updateConfig={onUpdate(EVENTS_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'URL Whitelist',
      shouldRender: urlWhiteListConfig,
      render: (key) => (
        isPermitted(currentUser.permissions, ['urlwhitelist:read']) && (
        <ConfigletContainer title="URL Whitelist Configuration" key={key}>
          <UrlWhiteListConfig config={urlWhiteListConfig}
                              updateConfig={onUpdate(URL_WHITELIST_CONFIG)} />
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
      shouldRender: permissionsConfig,
      render: (key) => (
        <ConfigletContainer title="Permissions Configuration" key={key}>
          <PermissionsConfig config={permissionsConfig}
                             updateConfig={onUpdate(PERMISSIONS_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Index Set Defaults',
      shouldRender: indexSetsDefaultsConfig,
      render: (key) => (
        <HideOnCloud key={key}>
          <ConfigletContainer title="Index Set Default Configuration">
            <IndexSetsDefaultsConfig initialConfig={indexSetsDefaultsConfig}
                                     updateConfig={onUpdate(INDEX_SETS_DEFAULTS_CONFIG)} />
          </ConfigletContainer>
        </HideOnCloud>
      ),
    },
    {
      name: 'Users',
      shouldRender: userConfig,
      render: (key) => (
        <ConfigletContainer title="User Configuration" key={key}>
          <UserConfig config={userConfig}
                      updateConfig={onUpdate(USER_CONFIG)} />
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
