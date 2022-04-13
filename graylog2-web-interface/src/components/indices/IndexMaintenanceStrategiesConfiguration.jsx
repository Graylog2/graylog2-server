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

const TIME_BASED_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy';
const NOOP_RETENTION_STRATEGY = 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy';

class IndexMaintenanceStrategiesConfiguration extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    selectPlaceholder: PropTypes.string.isRequired,
    pluginExports: PropTypes.array.isRequired,
    strategies: PropTypes.array.isRequired,
    retentionStrategiesContext: PropTypes.shape({
      max_index_retention_period: PropTypes.string,
    }),
    activeConfig: PropTypes.object.isRequired,
    updateState: PropTypes.func.isRequired,
  };

  static defaultProps = {
    retentionStrategiesContext: {
      max_index_retention_period: undefined,
    },
  };

  constructor(props) {
    super(props);
    const { activeConfig: { strategy, config } } = this.props;

    this.state = {
      activeStrategy: strategy,
      activeConfig: config,
      newStrategy: strategy,
    };
  }

  _getDefaultStrategyConfig = (selectedStrategy) => {
    const { strategies } = this.props;
    const result = strategies.filter((strategy) => strategy.type === selectedStrategy)[0];

    return result ? result.default_config : undefined;
  };

  _getStrategyJsonSchema = (selectedStrategy) => {
    const { strategies } = this.props;
    const result = strategies.filter((strategy) => strategy.type === selectedStrategy)[0];

    return result ? result.json_schema : undefined;
  };

  _getTimeBaseStrategyWithElasticLimit = () => {
    const { activeConfig } = this.state;
    const timeBasedStrategy = this._getDefaultStrategyConfig(TIME_BASED_ROTATION_STRATEGY);

    return { ...activeConfig, max_rotation_period: timeBasedStrategy?.max_rotation_period };
  };

  _getStrategyConfig = (selectedStrategy) => {
    const { activeStrategy, activeConfig } = this.state;

    if (activeStrategy === selectedStrategy) {
      // If the newly selected strategy is the current active strategy, we use the active configuration.
      return activeStrategy === TIME_BASED_ROTATION_STRATEGY ? this._getTimeBaseStrategyWithElasticLimit() : activeConfig;
    }

    // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
    return this._getDefaultStrategyConfig(selectedStrategy);
  };

  _onSelect = (newStrategy) => {
    const { updateState } = this.props;

    if (!newStrategy || newStrategy.length < 1) {
      this.setState({ newStrategy: undefined });

      return;
    }

    const newConfig = this._getStrategyConfig(newStrategy);

    this.setState({ newStrategy: newStrategy });
    updateState(newStrategy, newConfig);
  };

  _addConfigType = (strategy, data) => {
    const { strategies } = this.props;
    // The config object needs to have the "type" field set to the "default_config.type" to make the REST call work.
    const result = strategies.filter((s) => s.type === strategy)[0];
    const copy = data;

    if (result) {
      copy.type = result.default_config.type;
    }

    return copy;
  };

  _onConfigUpdate = (newConfig) => {
    const { newStrategy } = this.state;
    const { updateState } = this.props;
    const config = this._addConfigType(newStrategy, newConfig);

    updateState(newStrategy, config);
  };

  _availableSelectOptions = () => {
    const { pluginExports, strategies } = this.props;

    return pluginExports
      .filter((config) => strategies.find(({ type }) => type === config.type))
      .map((config) => {
        return { value: config.type, label: config.displayName };
      });
  };

  _getConfigurationComponent = (selectedStrategy) => {
    const { pluginExports } = this.props;

    if (!selectedStrategy || selectedStrategy.length < 1) {
      return null;
    }

    const strategy = pluginExports.filter((exportedStrategy) => exportedStrategy.type === selectedStrategy)[0];

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
    const { newStrategy } = this.state;

    return newStrategy;
  };

  render() {
    const { title, description, selectPlaceholder } = this.props;

    return (
      <span>
        <h3>{title}</h3>
        <p className="description">{description}</p>
        <Input id="strategy-select" label={selectPlaceholder}>
          <Select placeholder={selectPlaceholder}
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
