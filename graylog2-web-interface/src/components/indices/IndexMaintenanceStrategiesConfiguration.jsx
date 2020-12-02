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
import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

class IndexMaintenanceStrategiesConfiguration extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    selectPlaceholder: PropTypes.string.isRequired,
    pluginExports: PropTypes.array.isRequired,
    strategies: PropTypes.array.isRequired,
    activeConfig: PropTypes.object.isRequired,
    updateState: PropTypes.func.isRequired,
  };

  state = {
    activeStrategy: this.props.activeConfig.strategy,
    activeConfig: this.props.activeConfig.config,
    newStrategy: this.props.activeConfig.strategy,
    newConfig: this.props.activeConfig.config,
  };

  _getDefaultStrategyConfig = (selectedStrategy) => {
    const result = this.props.strategies.filter((strategy) => strategy.type === selectedStrategy)[0];

    return result ? result.default_config : undefined;
  };

  _getStrategyJsonSchema = (selectedStrategy) => {
    const result = this.props.strategies.filter((strategy) => strategy.type === selectedStrategy)[0];

    return result ? result.json_schema : undefined;
  };

  _getStrategyConfig = (selectedStrategy) => {
    if (this.state.activeStrategy === selectedStrategy) {
      // If the newly selected strategy is the current active strategy, we use the active configuration.
      return this.state.activeConfig;
    }

    // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
    return this._getDefaultStrategyConfig(selectedStrategy);
  };

  _onSelect = (newStrategy) => {
    if (!newStrategy || newStrategy.length < 1) {
      this.setState({ newStrategy: undefined });

      return;
    }

    const newConfig = this._getStrategyConfig(newStrategy);

    this.setState({ newStrategy: newStrategy, newConfig: newConfig });
    this.props.updateState(newStrategy, newConfig);
  };

  _addConfigType = (strategy, data) => {
    // The config object needs to have the "type" field set to the "default_config.type" to make the REST call work.
    const result = this.props.strategies.filter((s) => s.type === strategy)[0];
    const copy = data;

    if (result) {
      copy.type = result.default_config.type;
    }

    return copy;
  };

  _onConfigUpdate = (newConfig) => {
    const config = this._addConfigType(this.state.newStrategy, newConfig);

    this.setState({ newConfig: config });
    this.props.updateState(this.state.newStrategy, config);
  };

  _availableSelectOptions = () => {
    return this.props.pluginExports.map((config) => {
      return { value: config.type, label: config.displayName };
    });
  };

  _getConfigurationComponent = (selectedStrategy) => {
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
  };

  _activeSelection = () => {
    return this.state.newStrategy;
  };

  render() {
    return (
      <span>
        <h3>{this.props.title}</h3>
        <p className="description">{this.props.description}</p>
        <Input id="strategy-select" label={this.props.selectPlaceholder}>
          <Select placeholder={this.props.selectPlaceholder}
                  options={this._availableSelectOptions()}
                  matchProp="label"
                  value={this._activeSelection()}
                  onChange={this._onSelect} />
        </Input>
        {this._getConfigurationComponent(this._activeSelection())}
      </span>
    );
  }
}

export default IndexMaintenanceStrategiesConfiguration;
