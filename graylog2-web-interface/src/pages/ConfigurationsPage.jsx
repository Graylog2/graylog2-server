import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { PageHeader, Spinner } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';

import ConfigurationsStore from 'stores/configurations/ConfigurationsStore';
import ConfigurationActions from 'actions/configurations/ConfigurationActions';

import SearchesConfig from 'components/configurations/SearchesConfig';

import style from '!style!css!components/configurations/ConfigurationStyles.css';

const ConfigurationsPage = React.createClass({
  mixins: [Reflux.connect(ConfigurationsStore)],

  getInitialState() {
    return {
      configuration: null,
    };
  },

  componentDidMount() {
    ConfigurationActions.list(this.SEARCHES_CLUSTER_CONFIG);

    PluginStore.exports('systemConfigurations').forEach((systemConfig) => {
      ConfigurationActions.list(systemConfig.configType);
    });
  },

  SEARCHES_CLUSTER_CONFIG: 'org.graylog2.indexer.searches.SearchesClusterConfig',

  _getConfig(configType) {
    if (this.state.configuration && this.state.configuration[configType]) {
      return this.state.configuration[configType];
    } else {
      return null;
    }
  },

  _onUpdate(configType) {
    return (config) => {
      return ConfigurationActions.update(configType, config);
    };
  },

  _pluginConfigs() {
    return PluginStore.exports('systemConfigurations').map((systemConfig, idx) => {
      return React.createElement(systemConfig.component, {
        key: `system-configuration-${idx}`,
        config: this._getConfig(systemConfig.configType) || {},
        updateConfig: this._onUpdate(systemConfig.configType),
      });
    });
  },

  _pluginConfigRows() {
    const pluginConfigs = this._pluginConfigs();
    const rows = [];
    let idx = 0;

    // Put two plugin config components per row.
    while (pluginConfigs.length > 0) {
      idx++;
      rows.push(
        <Row key={`plugin-config-row-${idx}`}>
          <Col md={6}>
            {pluginConfigs.shift()}
          </Col>
          <Col md={6}>
            {pluginConfigs.shift() || (<span>&nbsp;</span>)}
          </Col>
        </Row>
      );
    }

    return rows;
  },

  render() {
    const searchesConfig = this._getConfig(this.SEARCHES_CLUSTER_CONFIG);
    let searchesConfigComponent;
    if (searchesConfig) {
      searchesConfigComponent = (
        <SearchesConfig config={searchesConfig}
                        updateConfig={this._onUpdate(this.SEARCHES_CLUSTER_CONFIG)} />
      );
    } else {
      searchesConfigComponent = (<Spinner />);
    }

    const pluginConfigRows = this._pluginConfigRows();

    return (
      <span>
        <PageHeader title="Configurations">
          <span>
            You can configure system settings for different sub systems on this page.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {searchesConfigComponent}
          </Col>
        </Row>

        {pluginConfigRows.length > 0 && <Row className="content">
          <Col md={12}>
            <h2>Plugins</h2>
            <p className="description">Configuration for installed plugins.</p>
            <hr className={style.separator} />
            <div className={style.topMargin}>
              {pluginConfigRows}
            </div>
          </Col>
        </Row>}
      </span>
    );
  },
});

export default ConfigurationsPage;
