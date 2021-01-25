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

  _pluginConfigs = () => {
    return PluginStore.exports('systemConfigurations').map((systemConfig, idx) => {
      return React.createElement(systemConfig.component, {
        // eslint-disable-next-line react/no-array-index-key
        key: `system-configuration-${idx}`,
        config: this._getConfig(systemConfig.configType) || undefined,
        updateConfig: this._onUpdate(systemConfig.configType),
      });
    });
  };

  _pluginConfigRows = () => {
    const pluginConfigs = this._pluginConfigs();
    const rows = [];
    let idx = 0;

    // Put two plugin config components per row.
    while (pluginConfigs.length > 0) {
      idx += 1;

      rows.push(
        <Row key={`plugin-config-row-${idx}`}>
          <Col md={6}>
            {pluginConfigs.shift()}
          </Col>
          <Col md={6}>
            {pluginConfigs.shift() || (<span>&nbsp;</span>)}
          </Col>
        </Row>,
      );
    }

    return rows;
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
          <Col md={6}>
            <SearchesConfig config={searchesConfig}
                            updateConfig={this._onUpdate(SEARCHES_CLUSTER_CONFIG)} />
          </Col>
          )}
          {messageProcessorsConfig && (
          <Col md={6}>
            <MessageProcessorsConfig config={messageProcessorsConfig}
                                     updateConfig={this._onUpdate(MESSAGE_PROCESSORS_CONFIG)} />
          </Col>
          )}
          {sidecarConfig && (
          <Col md={6}>
            <SidecarConfig config={sidecarConfig}
                           updateConfig={this._onUpdate(SIDECAR_CONFIG)} />
          </Col>
          )}
          {eventsConfig && (
          <Col md={6}>
            <EventsConfig config={eventsConfig}
                          updateConfig={this._onUpdate(EVENTS_CONFIG)} />
          </Col>
          )}
          {isPermitted(permissions, ['urlwhitelist:read']) && urlWhiteListConfig && (
          <Col md={6}>
            <UrlWhiteListConfig config={urlWhiteListConfig}
                                updateConfig={this._onUpdate(URL_WHITELIST_CONFIG)} />
          </Col>
          )}
          <Col md={6}>
            <DecoratorsConfig />
          </Col>
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
