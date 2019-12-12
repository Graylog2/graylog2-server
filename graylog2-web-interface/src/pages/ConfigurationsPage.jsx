import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlWhiteListConfig from 'components/configurations/UrlWhiteListConfig';

import {} from 'components/maps/configurations';

import 'components/configurations/ConfigurationStyles.css';

const ConfigurationsStore = StoreProvider.getStore('Configurations');
const ConfigurationActions = ActionsProvider.getActions('Configuration');

class ConfigurationsPage extends React.Component {
   SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig'

   MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig'

   SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration'

   EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration'

   URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist'

   componentDidMount() {
     ConfigurationActions.list(this.SEARCHES_CLUSTER_CONFIG);
     ConfigurationActions.listMessageProcessorsConfig(this.MESSAGE_PROCESSORS_CONFIG);
     ConfigurationActions.list(this.SIDECAR_CONFIG);
     ConfigurationActions.list(this.EVENTS_CONFIG);
     ConfigurationActions.listWhiteListConfig(this.URL_WHITELIST_CONFIG);

     PluginStore.exports('systemConfigurations').forEach((systemConfig) => {
       ConfigurationActions.list(systemConfig.configType);
     });
   }

   componentWillUnmount() {
   }


  _getConfig = (configType) => {
    const { configuration } = this.props;
    if (configuration && configuration[configType]) {
      return configuration[configType];
    }
    return null;
  }

  _onUpdate = (configType) => {
    return (config) => {
      switch (configType) {
        case this.MESSAGE_PROCESSORS_CONFIG:
          return ConfigurationActions.updateMessageProcessorsConfig(configType, config);
        default:
          return ConfigurationActions.update(configType, config);
      }
    };
  }

  _pluginConfigs = () => {
    return PluginStore.exports('systemConfigurations').map((systemConfig, idx) => {
      return React.createElement(systemConfig.component, {
        // eslint-disable-next-line react/no-array-index-key
        key: `system-configuration-${idx}`,
        config: this._getConfig(systemConfig.configType) || undefined,
        updateConfig: this._onUpdate(systemConfig.configType),
      });
    });
  }

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
  }

  render() {
    const searchesConfig = this._getConfig(this.SEARCHES_CLUSTER_CONFIG);
    const messageProcessorsConfig = this._getConfig(this.MESSAGE_PROCESSORS_CONFIG);
    const sidecarConfig = this._getConfig(this.SIDECAR_CONFIG);
    const eventsConfig = this._getConfig(this.EVENTS_CONFIG);
    const urlwhitelistConfig = this._getConfig(this.URL_WHITELIST_CONFIG);
    let searchesConfigComponent;
    let messageProcessorsConfigComponent;
    let sidecarConfigComponent;
    let eventsConfigComponent;
    let urlwhitelistConfigComponent;
    if (searchesConfig) {
      searchesConfigComponent = (
        <SearchesConfig config={searchesConfig}
                        updateConfig={this._onUpdate(this.SEARCHES_CLUSTER_CONFIG)} />
      );
    } else {
      searchesConfigComponent = (<Spinner />);
    }
    if (messageProcessorsConfig) {
      messageProcessorsConfigComponent = (
        <MessageProcessorsConfig config={messageProcessorsConfig}
                                 updateConfig={this._onUpdate(this.MESSAGE_PROCESSORS_CONFIG)} />
      );
    } else {
      messageProcessorsConfigComponent = (<Spinner />);
    }
    if (sidecarConfig) {
      sidecarConfigComponent = (
        <SidecarConfig config={sidecarConfig}
                       updateConfig={this._onUpdate(this.SIDECAR_CONFIG)} />
      );
    } else {
      sidecarConfigComponent = (<Spinner />);
    }
    if (eventsConfig) {
      eventsConfigComponent = (
        <EventsConfig config={eventsConfig}
                      updateConfig={this._onUpdate(this.EVENTS_CONFIG)} />
      );
    } else {
      eventsConfigComponent = (<Spinner />);
    }
    if (urlwhitelistConfig) {
      urlwhitelistConfigComponent = (
        <UrlWhiteListConfig config={urlwhitelistConfig}
                            updateConfig={this._onUpdate(this.URL_WHITELIST_CONFIG)} />
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
              {urlwhitelistConfigComponent}
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


ConfigurationsPage.propTypes = {
  configuration: PropTypes.object.isRequired,
};

export default connect(ConfigurationsPage, { configurationsStore: ConfigurationsStore }, ({ configurationsStore, ...otherProps }) => ({
  ...configurationsStore,
  ...otherProps,
}));
