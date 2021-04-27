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

import { Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { isPermitted } from 'util/PermissionsMixin';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlWhiteListConfig from 'components/configurations/UrlWhiteListConfig';
import {} from 'components/maps/configurations';
import style from 'components/configurations/ConfigurationStyles.lazy.css';
import { Store } from 'stores/StoreTypes';
import usePluginEntities from 'views/logic/usePluginEntities';

import ConfigletContainer from './configurations/ConfigletContainer';
import PluginConfigRows from './configurations/PluginConfigRows';

import DecoratorsConfig from '../components/configurations/DecoratorsConfig';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig';
const MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig';
const SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration';
const EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration';
const URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist';

const _getConfig = (configType, configuration) => configuration?.[configType] ?? null;

const _onUpdate = (configType: string) => {
  return (config) => {
    switch (configType) {
      case MESSAGE_PROCESSORS_CONFIG:
        return ConfigurationsActions.updateMessageProcessorsConfig(configType, config);
      case URL_WHITELIST_CONFIG:
        return ConfigurationsActions.updateWhitelist(configType, config);
      default:
        return ConfigurationsActions.update(configType, config);
    }
  };
};

const ConfigurationsPage = () => {
  const [loaded, setLoaded] = useState(false);
  const pluginSystemConfigs = usePluginEntities('systemConfigurations');
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const permissions = useStore(CurrentUserStore as Store<{ currentUser: { permissions: Array<string> } }>, (state) => state?.currentUser?.permissions);

  useEffect(() => {
    style.use();

    return () => { style.unuse(); };
  }, []);

  useEffect(() => {
    const promises = [
      ConfigurationsActions.list(SEARCHES_CLUSTER_CONFIG),
      ConfigurationsActions.listMessageProcessorsConfig(MESSAGE_PROCESSORS_CONFIG),
      ConfigurationsActions.list(SIDECAR_CONFIG),
      ConfigurationsActions.list(EVENTS_CONFIG),
    ];

    if (isPermitted(permissions, ['urlwhitelist:read'])) {
      promises.push(ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG));
    }

    const pluginPromises = pluginSystemConfigs
      .map((systemConfig) => ConfigurationsActions.list(systemConfig.configType));

    Promise.allSettled([...promises, ...pluginPromises]).then(() => setLoaded(true));
  }, [permissions, pluginSystemConfigs]);

  let Output = (
    <Col md={12}>
      <Spinner text="Loading Configuration Panel..." />
    </Col>
  );

  if (loaded) {
    const searchesConfig = _getConfig(SEARCHES_CLUSTER_CONFIG, configuration);
    const messageProcessorsConfig = _getConfig(MESSAGE_PROCESSORS_CONFIG, configuration);
    const sidecarConfig = _getConfig(SIDECAR_CONFIG, configuration);
    const eventsConfig = _getConfig(EVENTS_CONFIG, configuration);
    const urlWhiteListConfig = _getConfig(URL_WHITELIST_CONFIG, configuration);

    Output = (
      <>
        {searchesConfig && (
          <ConfigletContainer title="Search Configuration">
            <SearchesConfig config={searchesConfig}
                            updateConfig={_onUpdate(SEARCHES_CLUSTER_CONFIG)} />
          </ConfigletContainer>
        )}
        {messageProcessorsConfig && (
          <ConfigletContainer title="Message Processor Configuration">
            <MessageProcessorsConfig config={messageProcessorsConfig}
                                     updateConfig={_onUpdate(MESSAGE_PROCESSORS_CONFIG)} />
          </ConfigletContainer>
        )}
        {sidecarConfig && (
          <ConfigletContainer title="Sidecar Configuration">
            <SidecarConfig config={sidecarConfig}
                           updateConfig={_onUpdate(SIDECAR_CONFIG)} />
          </ConfigletContainer>
        )}
        {eventsConfig && (
          <ConfigletContainer title="Events Configuration">
            <EventsConfig config={eventsConfig}
                          updateConfig={_onUpdate(EVENTS_CONFIG)} />
          </ConfigletContainer>
        )}
        {isPermitted(permissions, ['urlwhitelist:read']) && urlWhiteListConfig && (
          <ConfigletContainer title="URL Whitelist Configuration">
            <UrlWhiteListConfig config={urlWhiteListConfig}
                                updateConfig={_onUpdate(URL_WHITELIST_CONFIG)} />
          </ConfigletContainer>
        )}
        <ConfigletContainer title="Decorators Configuration">
          <DecoratorsConfig />
        </ConfigletContainer>
      </>
    );
  }

  return (
    <DocumentTitle title="Configurations">
      <span>
        <PageHeader title="Configurations">
          <span>
            You can configure system settings for different sub systems on this page.
          </span>
        </PageHeader>

        <Row className="content">
          {Output}
        </Row>

        {pluginSystemConfigs.length > 0 && (
        <Row className="content">
          <Col md={12}>
            <h2>Plugins</h2>
            <p className="description">Configuration for installed plugins.</p>
            <hr className="separator" />
            <div className="top-margin">
              <PluginConfigRows configuration={configuration} systemConfigs={pluginSystemConfigs} />
            </div>
          </Col>
        </Row>
        )}
      </span>
    </DocumentTitle>
  );
};

export default ConfigurationsPage;
