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
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
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
      case ConfigurationType.URL_WHITELIST_CONFIG:
        return ConfigurationsActions.updateWhitelist(configType, config);
      case ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG:
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
      ConfigurationsActions.list(ConfigurationType.SIDECAR_CONFIG),
      ConfigurationsActions.list(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG),
      ConfigurationsActions.list(ConfigurationType.EVENTS_CONFIG),
      ConfigurationsActions.listPermissionsConfig(ConfigurationType.PERMISSIONS_CONFIG),
      ConfigurationsActions.listUserConfig(ConfigurationType.USER_CONFIG),
    ];

    if (isPermitted(currentUser.permissions, ['urlwhitelist:read'])) {
      promises.push(ConfigurationsActions.listWhiteListConfig(ConfigurationType.URL_WHITELIST_CONFIG));
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

  const sidecarConfig = getConfig(ConfigurationType.SIDECAR_CONFIG, configuration);
  const eventsConfig = getConfig(ConfigurationType.EVENTS_CONFIG, configuration);
  const urlWhiteListConfig = getConfig(ConfigurationType.URL_WHITELIST_CONFIG, configuration);
  const indexSetsDefaultsConfig = getConfig(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG, configuration);
  const permissionsConfig = getConfig(ConfigurationType.PERMISSIONS_CONFIG, configuration);
  const userConfig = getConfig(ConfigurationType.USER_CONFIG, configuration);

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
      shouldRender: sidecarConfig,
      render: (key) => (
        <ConfigletContainer title="Sidecar Configuration" key={key}>
          <SidecarConfig config={sidecarConfig}
                         updateConfig={onUpdate(ConfigurationType.SIDECAR_CONFIG)} />
        </ConfigletContainer>
      ),
    },
    {
      name: 'Events',
      shouldRender: eventsConfig,
      render: (key) => (
        <ConfigletContainer title="Events Configuration" key={key}>
          <EventsConfig config={eventsConfig}
                        updateConfig={onUpdate(ConfigurationType.EVENTS_CONFIG)} />
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
                              updateConfig={onUpdate(ConfigurationType.URL_WHITELIST_CONFIG)} />
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
                             updateConfig={onUpdate(ConfigurationType.PERMISSIONS_CONFIG)} />
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
                                     updateConfig={onUpdate(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG)} />
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
                      updateConfig={onUpdate(ConfigurationType.USER_CONFIG)} />
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
