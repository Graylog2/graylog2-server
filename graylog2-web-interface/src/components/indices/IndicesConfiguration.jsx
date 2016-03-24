import React from 'react';
import Reflux from 'reflux';

import { Button, Row, Col } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Spinner from 'components/common/Spinner';
import { PluginStore } from 'graylog-web-plugin/plugin';

import ActionsProvider from 'injection/ActionsProvider';
const IndicesConfigurationActions = ActionsProvider.getActions('IndicesConfiguration');

import StoreProvider from 'injection/StoreProvider';
const IndicesConfigurationStore = StoreProvider.getStore('IndicesConfiguration');

import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import IndexMaintenanceStrategiesSummary from 'components/indices/IndexMaintenanceStrategiesSummary';
import {} from 'components/indices/rotation'; // Load rotation plugin UI plugins from core.
import {} from 'components/indices/retention'; // Load rotation plugin UI plugins from core.

const IndicesConfiguration = React.createClass({
  mixins: [Reflux.connect(IndicesConfigurationStore)],

  componentDidMount() {
    this.style.use();
    IndicesConfigurationActions.loadRotationConfig();
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionConfig();
    IndicesConfigurationActions.loadRetentionStrategies();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!components/configurations/ConfigurationStyles.css'),

  _saveConfiguration() {
    const promises = [];

    if (this.state.newRotationConfig) {
      const promise = IndicesConfigurationActions.updateRotationConfiguration(this.state.newRotationConfig);
      promises.push(promise);
      // Delete the new state once it has been saved
      promise.then(() => this.setState({newRotationConfig: undefined}));
    }
    if (this.state.newRetentionConfig) {
      const promise = IndicesConfigurationActions.updateRetentionConfiguration(this.state.newRetentionConfig);
      promises.push(promise);
      // Delete the new state once it has been saved
      promise.then(() => this.setState({newRetentionConfig: undefined}));
    }

    Promise.all(promises).then(() => {
      this.refs.indicesConfigurationModal.close();
    });
  },

  _openModal() {
    this.refs.indicesConfigurationModal.open();
  },

  _updateRotationConfigState(strategy, config) {
    this.setState({
      newRotationConfig: {
        strategy: strategy,
        config: config,
      },
    });
  },

  _updateRetentionConfigState(strategy, config) {
    this.setState({
      newRetentionConfig: {
        strategy: strategy,
        config: config,
      },
    });
  },

  render() {
    let rotationSummary;
    if (this.state.activeRotationConfig) {
      rotationSummary = (<IndexMaintenanceStrategiesSummary config={this.state.activeRotationConfig}
                                                            pluginExports={PluginStore.exports('indexRotationConfig')} />);
    } else {
      rotationSummary = (<Spinner />);
    }

    let retentionSummary;
    if (this.state.activeRetentionConfig) {
      retentionSummary = (<IndexMaintenanceStrategiesSummary config={this.state.activeRetentionConfig}
                                                             pluginExports={PluginStore.exports('indexRetentionConfig')} />);
    } else {
      retentionSummary = (<Spinner />);
    }

    let rotationConfig;
    if (this.state.activeRotationConfig && this.state.rotationStrategies) {
      rotationConfig = (<IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                                 description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                                                 selectPlaceholder="Select rotation strategy"
                                                                 pluginExports={PluginStore.exports('indexRotationConfig')}
                                                                 strategies={this.state.rotationStrategies}
                                                                 activeConfig={this.state.activeRotationConfig}
                                                                 updateState={this._updateRotationConfigState} />);
    } else {
      rotationConfig = (<Spinner />);
    }

    let retentionConfig;
    if (this.state.activeRetentionConfig && this.state.retentionStrategies) {
      retentionConfig = (<IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                                  description="Graylog uses a retention strategy to clean up old indices."
                                                                  selectPlaceholder="Select retention strategy"
                                                                  pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                                  strategies={this.state.retentionStrategies}
                                                                  activeConfig={this.state.activeRetentionConfig}
                                                                  updateState={this._updateRetentionConfigState} />);
    } else {
      retentionConfig = (<Spinner />);
    }

    return (
      <div>
        <h2>Settings</h2>

        <div className="top-margin">
          <Row>
            <Col md={6}>
              {rotationSummary}
            </Col>
            <Col md={6}>
              {retentionSummary}
            </Col>
          </Row>
          <hr className="separator"/>
          <Button bsStyle="info" bsSize="xs" onClick={() => this._openModal()}>Update configuration</Button>{' '}
        </div>

        <BootstrapModalForm ref="indicesConfigurationModal"
                            title="Update Index Settings"
                            onSubmitForm={this._saveConfiguration}
                            submitButtonText="Save">
          {rotationConfig}
          <hr/>
          {retentionConfig}
        </BootstrapModalForm>
      </div>
    );
  },
});

export default IndicesConfiguration;
