import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import DecoratorsConfig from '../components/configurations/DecoratorsConfig';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!components/configurations/ConfigurationStyles.css';

const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig';
const MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig';
const SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration';
const EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration';

class ConfigurationsPage extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
  };

  componentDidMount() {
    style.use();
    ConfigurationsActions.list(SEARCHES_CLUSTER_CONFIG);
    ConfigurationsActions.listMessageProcessorsConfig(MESSAGE_PROCESSORS_CONFIG);
    ConfigurationsActions.list(SIDECAR_CONFIG);
    ConfigurationsActions.list(EVENTS_CONFIG);

    PluginStore.exports('systemConfigurations').forEach((systemConfig) => {
      ConfigurationsActions.list(systemConfig.configType);
    });
  }

  componentWillUnmount() {
    style.unuse();
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
      // eslint-disable-next-line no-plusplus
      idx++;
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

  render() {
    const searchesConfig = this._getConfig(SEARCHES_CLUSTER_CONFIG);
    const messageProcessorsConfig = this._getConfig(MESSAGE_PROCESSORS_CONFIG);
    const sidecarConfig = this._getConfig(SIDECAR_CONFIG);
    const eventsConfig = this._getConfig(EVENTS_CONFIG);
    let searchesConfigComponent;
    let messageProcessorsConfigComponent;
    let sidecarConfigComponent;
    let eventsConfigComponent;
    if (searchesConfig) {
      searchesConfigComponent = (
        <SearchesConfig config={searchesConfig}
                        updateConfig={this._onUpdate(SEARCHES_CLUSTER_CONFIG)} />
      );
    } else {
      searchesConfigComponent = (<Spinner />);
    }
    if (messageProcessorsConfig) {
      messageProcessorsConfigComponent = (
        <MessageProcessorsConfig config={messageProcessorsConfig}
                                 updateConfig={this._onUpdate(MESSAGE_PROCESSORS_CONFIG)} />
      );
    } else {
      messageProcessorsConfigComponent = (<Spinner />);
    }
    if (sidecarConfig) {
      sidecarConfigComponent = (
        <SidecarConfig config={sidecarConfig}
                       updateConfig={this._onUpdate(SIDECAR_CONFIG)} />
      );
    } else {
      sidecarConfigComponent = (<Spinner />);
    }
    if (eventsConfig) {
      eventsConfigComponent = (
        <EventsConfig config={eventsConfig}
                      updateConfig={this._onUpdate(EVENTS_CONFIG)} />
      );
    } else {
      eventsConfigComponent = (<Spinner />);
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
            <Col md={6}>
              {searchesConfigComponent}
            </Col>
            <Col md={6}>
              {messageProcessorsConfigComponent}
            </Col>
            <Col md={6}>
              {sidecarConfigComponent}
            </Col>
            <Col md={6}>
              {eventsConfigComponent}
            </Col>
            <Col md={6}>
              <DecoratorsConfig />
            </Col>
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

export default connect(ConfigurationsPage, { configurations: ConfigurationsStore }, ({ configurations: { configuration } = {} }) => ({ configuration }));
