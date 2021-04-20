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
import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { chunk } from 'lodash';
import { ErrorBoundary } from 'react-error-boundary';

import { Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { isPermitted } from 'util/PermissionsMixin';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlWhiteListConfig from 'components/configurations/UrlWhiteListConfig';
import {} from 'components/maps/configurations';
import style from 'components/configurations/ConfigurationStyles.lazy.css';

import DecoratorsConfig from '../components/configurations/DecoratorsConfig';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig';
const MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig';
const SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration';
const EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration';
const URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist';

const ErrorFallback = ({ error, title }) => (
  <>
    <h2>{title}</h2>
    <p>Something went wrong:</p>
    <pre>{error.message}</pre>
  </>
);

const Boundary = ({ children, title }) => <ErrorBoundary FallbackComponent={(props) => <ErrorFallback title={title} {...props} />}>{children}</ErrorBoundary>;
const ConfigletContainer = ({ children, title }) => (
  <Col md={6}>
    <Boundary title={title}>
      {children}
    </Boundary>
  </Col>
);

class ConfigurationsPage extends React.Component {
  checkLoadedTimer = undefined

  constructor(props) {
    super(props);

    this.state = { loaded: false };
  }

  componentDidMount() {
    style.use();
    const { currentUser: { permissions } } = this.props;

    this._checkConfig();

    ConfigurationsActions.list(SEARCHES_CLUSTER_CONFIG);
    ConfigurationsActions.listMessageProcessorsConfig(MESSAGE_PROCESSORS_CONFIG);
    ConfigurationsActions.list(SIDECAR_CONFIG);
    ConfigurationsActions.list(EVENTS_CONFIG);

    if (isPermitted(permissions, ['urlwhitelist:read'])) {
      ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG);
    }

    PluginStore.exports('systemConfigurations').forEach((systemConfig) => {
      ConfigurationsActions.list(systemConfig.configType);
    });
  }

  componentWillUnmount() {
    style.unuse();
    this._clearTimeout();
  }

  _getConfig = (configType) => {
    const { configuration } = this.props;

    if (configuration && configuration[configType]) {
      return configuration[configType];
    }

    return null;
  };

  _onUpdate = (configType) => {
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

  _pluginConfigs = () => PluginStore.exports('systemConfigurations')
    .map(({ component: SystemConfigComponent, configType }, idx) => (
      <ConfigletContainer title={configType}>
        <SystemConfigComponent key={`system-configuration-${idx}`}
                               config={this._getConfig(configType) ?? undefined}
                               updateConfig={this._onUpdate(configType)} />
      </ConfigletContainer>
    ));

  _pluginConfigRows = () => {
    const pluginConfigs = this._pluginConfigs();

    // Put two plugin config components per row.
    return chunk(pluginConfigs, 2)
      .map((configChunk, idx) => (
        <Row key={`plugin-config-row-${idx}`}>
          {configChunk[0]}
          {configChunk[1]}
        </Row>
      ));
  };

  _checkConfig = () => {
    const { configuration } = this.props;

    this.checkLoadedTimer = setTimeout(() => {
      if (Object.keys(configuration).length > 0) {
        this.setState({ loaded: true }, this._clearTimeout);

        return;
      }

      this._checkConfig();
    }, 100);
  };

  _clearTimeout = () => {
    if (this.checkLoadedTimer) {
      clearTimeout(this.checkLoadedTimer);
    }
  }

  render() {
    const { loaded } = this.state;
    const { currentUser: { permissions } } = this.props;
    let Output = (
      <Col md={12}>
        <Spinner text="Loading Configuration Panel..." />
      </Col>
    );

    if (loaded) {
      const searchesConfig = this._getConfig(SEARCHES_CLUSTER_CONFIG);
      const messageProcessorsConfig = this._getConfig(MESSAGE_PROCESSORS_CONFIG);
      const sidecarConfig = this._getConfig(SIDECAR_CONFIG);
      const eventsConfig = this._getConfig(EVENTS_CONFIG);
      const urlWhiteListConfig = this._getConfig(URL_WHITELIST_CONFIG);

      Output = (
        <>
          {searchesConfig && (
          <ConfigletContainer title="Search Configuration">
            <SearchesConfig config={searchesConfig}
                            updateConfig={this._onUpdate(SEARCHES_CLUSTER_CONFIG)} />
          </ConfigletContainer>
          )}
          {messageProcessorsConfig && (
          <ConfigletContainer title="Message Processor Configuration">
            <MessageProcessorsConfig config={messageProcessorsConfig}
                                     updateConfig={this._onUpdate(MESSAGE_PROCESSORS_CONFIG)} />
          </ConfigletContainer>
          )}
          {sidecarConfig && (
          <ConfigletContainer title="Sidecar Configuration">
            <SidecarConfig config={sidecarConfig}
                           updateConfig={this._onUpdate(SIDECAR_CONFIG)} />
          </ConfigletContainer>
          )}
          {eventsConfig && (
          <ConfigletContainer title="Events Configuration">
            <EventsConfig config={eventsConfig}
                          updateConfig={this._onUpdate(EVENTS_CONFIG)} />
          </ConfigletContainer>
          )}
          {isPermitted(permissions, ['urlwhitelist:read']) && urlWhiteListConfig && (
          <ConfigletContainer title="URL Whitelist Configuration">
            <UrlWhiteListConfig config={urlWhiteListConfig}
                                updateConfig={this._onUpdate(URL_WHITELIST_CONFIG)} />
          </ConfigletContainer>
          )}
          <ConfigletContainer title="Decorators Configuration">
            <DecoratorsConfig />
          </ConfigletContainer>
        </>
      );
    }

    const pluginConfigRows = this._pluginConfigRows();

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

          {pluginConfigRows.length > 0 && (
          <Row className="content">
            <Col md={12}>
              <h2>Plugins</h2>
              <p className="description">Configuration for installed plugins.</p>
              <hr className="separator" />
              <div className="top-margin">
                {pluginConfigRows}
              </div>
            </Col>
          </Row>
          )}
        </span>
      </DocumentTitle>
    );
  }
}

ConfigurationsPage.propTypes = {
  configuration: PropTypes.object.isRequired,
  currentUser: PropTypes.object.isRequired,
};

export default connect(ConfigurationsPage, { configurations: ConfigurationsStore, currentUser: CurrentUserStore }, ({ configurations, currentUser, ...otherProps }) => ({
  ...configurations,
  ...currentUser,
  ...otherProps,
}));
