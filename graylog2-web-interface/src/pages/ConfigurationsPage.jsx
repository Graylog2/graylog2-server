import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { PageHeader, Spinner } from 'components/common';

import ConfigurationsStore from 'stores/configurations/ConfigurationsStore';
import ConfigurationActions from 'actions/configurations/ConfigurationActions';

import SearchesConfig from 'components/configurations/SearchesConfig';

const ConfigurationsPage = React.createClass({
  mixins: [Reflux.connect(ConfigurationsStore)],

  getInitialState() {
    return {
      configuration: null,
    };
  },

  componentDidMount() {
    ConfigurationActions.list(this.SEARCHES_CLUSTER_CONFIG);
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
      </span>
    );
  },
});

export default ConfigurationsPage;
