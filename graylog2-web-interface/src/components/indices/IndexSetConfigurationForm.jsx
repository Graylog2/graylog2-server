import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Input } from 'react-bootstrap';

import { Spinner } from 'components/common';

import { PluginStore } from 'graylog-web-plugin/plugin';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import {} from 'components/indices/rotation'; // Load rotation plugin UI plugins from core.
import {} from 'components/indices/retention'; // Load rotation plugin UI plugins from core.

const IndexSetConfigurationForm = React.createClass({
  propTypes: {
    indexSet: React.PropTypes.object.isRequired,
    rotationStrategies: React.PropTypes.array.isRequired,
    retentionStrategies: React.PropTypes.array.isRequired,
    create: React.PropTypes.bool,
    onUpdate: React.PropTypes.func.isRequired,
    cancelLink: React.PropTypes.string.isRequired,
  },

  getInitialState() {
    return {
      indexSet: this.props.indexSet,
    };
  },

  _updateConfig(fieldName, value) {
    const config = this.state.indexSet;
    config[fieldName] = value;
    this.setState({ indexSet: config });
  },

  _onInputChange(event) {
    this._updateConfig(event.target.name, event.target.value);
  },

  _saveConfiguration(event) {
    event.preventDefault();
    this.props.onUpdate(this.state.indexSet);
  },

  _updateRotationConfigState(strategy, data) {
    this._updateConfig('rotation_strategy_class', strategy);
    this._updateConfig('rotation_strategy', data);
  },

  _updateRetentionConfigState(strategy, data) {
    this._updateConfig('retention_strategy_class', strategy);
    this._updateConfig('retention_strategy', data);
  },

  render() {
    const indexSet = this.props.indexSet;

    let rotationConfig;
    if (this.props.rotationStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: this.props.indexSet.rotation_strategy,
        strategy: this.props.indexSet.rotation_strategy_class,
      };
      rotationConfig = (<IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                                 description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                                                 selectPlaceholder="Select rotation strategy"
                                                                 pluginExports={PluginStore.exports('indexRotationConfig')}
                                                                 strategies={this.props.rotationStrategies}
                                                                 activeConfig={activeConfig}
                                                                 updateState={this._updateRotationConfigState} />);
    } else {
      rotationConfig = (<Spinner />);
    }

    let retentionConfig;
    if (this.props.retentionStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: this.props.indexSet.retention_strategy,
        strategy: this.props.indexSet.retention_strategy_class,
      };
      retentionConfig = (<IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                                  description="Graylog uses a retention strategy to clean up old indices."
                                                                  selectPlaceholder="Select retention strategy"
                                                                  pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                                  strategies={this.props.retentionStrategies}
                                                                  activeConfig={activeConfig}
                                                                  updateState={this._updateRetentionConfigState} />);
    } else {
      retentionConfig = (<Spinner />);
    }

    let readOnlyconfig;
    if (this.props.create) {
      readOnlyconfig = (
        <span>
          <Input type="text"
                 id="index-set-index-prefix"
                 label="Index prefix"
                 name="index_prefix"
                 onChange={this._onInputChange}
                 value={indexSet.index_prefix}
                 help="The prefix for all indices in this index set."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-7"
                 required />
          <Input type="number"
                 id="index-set-shards"
                 label="Index shards"
                 name="shards"
                 onChange={this._onInputChange}
                 value={indexSet.shards}
                 help="Number of shards per index."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-7"
                 required />
          <Input type="number"
                 id="index-set-replicas"
                 label="Index replicas"
                 name="replicas"
                 onChange={this._onInputChange}
                 value={indexSet.replicas}
                 help="Number of replicas per index."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-7"
                 required />
        </span>
      );
    }

    return (
      <form className="form form-horizontal index-set-form" onSubmit={this._saveConfiguration}>
        <fieldset>
          <Input type="text"
                 id="index-set-title"
                 label="Title"
                 name="title"
                 onChange={this._onInputChange}
                 value={indexSet.title}
                 help="Descriptive name of the index set."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-7"
                 autoFocus
                 required />
          <Input type="text"
                 id="index-set-description"
                 label="Description"
                 name="description"
                 onChange={this._onInputChange}
                 value={indexSet.description}
                 help="Description of the index set."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-7"
                 required />
          {readOnlyconfig}
          <Input wrapperClassName="col-sm-offset-3 col-sm-7">
            {indexSet.writable && rotationConfig}
            {indexSet.writable && retentionConfig}

            <hr/>
            <Button type="submit" bsStyle="success">Save</Button>
            &nbsp;
            <LinkContainer to={this.props.cancelLink}>
              <Button bsStyle="default">Cancel</Button>
            </LinkContainer>
          </Input>
        </fieldset>
      </form>
    );
  },
});

export default IndexSetConfigurationForm;
