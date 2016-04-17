import React from 'react';

import { Select } from 'components/common';

const IndexMaintenanceStrategiesConfiguration = React.createClass({
  propTypes: {
    title: React.PropTypes.string.isRequired,
    description: React.PropTypes.string.isRequired,
    selectPlaceholder: React.PropTypes.string.isRequired,
    pluginExports: React.PropTypes.array.isRequired,
    strategies: React.PropTypes.array.isRequired,
    activeConfig: React.PropTypes.object.isRequired,
    updateState: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      activeStrategy: this.props.activeConfig.strategy,
      activeConfig: this.props.activeConfig.config,
      newStrategy: this.props.activeConfig.strategy,
      newConfig: this.props.activeConfig.config,
    };
  },

  _getDefaultStrategyConfig(selectedStrategy) {
    const result = this.props.strategies.filter((strategy) => strategy.type === selectedStrategy)[0];
    return result ? result.default_config : undefined;
  },

  _getStrategyJsonSchema(selectedStrategy) {
    const result = this.props.strategies.filter((strategy) => strategy.type === selectedStrategy)[0];
    return result ? result.json_schema : undefined;
  },

  _getStrategyConfig(selectedStrategy) {
    if (this.state.activeStrategy === selectedStrategy) {
      // If the newly selected strategy is the current active strategy, we use the active configuration.
      return this.state.activeConfig;
    } else {
      // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
      return this._getDefaultStrategyConfig(selectedStrategy);
    }
  },

  _onSelect(newStrategy) {
    if (!newStrategy || newStrategy.length < 1) {
      this.setState({newStrategy: undefined});
      return;
    }

    const newConfig = this._getStrategyConfig(newStrategy);

    this.setState({newStrategy: newStrategy, newConfig: newConfig});
    this.props.updateState(newStrategy, newConfig);
  },

  _onConfigUpdate(newConfig) {
    this.setState({newConfig: newConfig});
    this.props.updateState(this.state.newStrategy, newConfig);
  },

  _availableSelectOptions() {
    return this.props.pluginExports.map((config) => {
      return {value: config.type, label: config.displayName};
    });
  },

  _getConfigurationComponent(selectedStrategy) {
    if (!selectedStrategy || selectedStrategy.length < 1) {
      return null;
    }

    const strategy = this.props.pluginExports.filter((exportedStrategy) => exportedStrategy.type === selectedStrategy)[0];

    if (!strategy) {
      return null;
    }

    const strategyConfig = this._getStrategyConfig(selectedStrategy);
    const element = React.createElement(strategy.configComponent, {
      config: strategyConfig,
      jsonSchema: this._getStrategyJsonSchema(selectedStrategy),
      updateConfig: this._onConfigUpdate,
    });

    return (<span key={strategy.type}>{element}</span>);
  },

  _activeSelection() {
    return this.state.newStrategy;
  },

  render() {
    return (
      <span>
        <h3>{this.props.title}</h3>
        <div className="top-margin">
          <p>{this.props.description}</p>
        </div>
        <div className="top-margin">
          <Select placeholder={this.props.selectPlaceholder}
                  options={this._availableSelectOptions()}
                  matchProp="value"
                  value={this._activeSelection()}
                  onValueChange={this._onSelect}/>
        </div>
        <div className="top-margin">
          {this._getConfigurationComponent(this._activeSelection())}
        </div>
      </span>
    );
  },
});

export default IndexMaintenanceStrategiesConfiguration;
