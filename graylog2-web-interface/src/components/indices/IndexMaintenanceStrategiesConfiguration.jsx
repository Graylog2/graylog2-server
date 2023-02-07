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
import * as React from 'react';
import { useCallback, useState } from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Alert, Col, Input, Row } from 'components/bootstrap';
import { Icon, Select } from 'components/common';

import {
  TIME_BASED_ROTATION_STRATEGY,
  TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY,
  TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY_TYPE,
  NOOP_RETENTION_STRATEGY,
  ARCHIVE_RETENTION_STRATEGY,
} from './Types';

const StyledH3 = styled.h3`
  margin-bottom: 10px;
`;
const StyledSelect = styled(Select)`
  margin-bottom: 10px;
`;
const StyledAlert = styled(Alert)`
  overflow: auto;
  margin-right: 15px;
  margin-left: 15px;
`;

const _getStrategyJsonSchema = (selectedStrategy, strategies) => {
  const result = strategies.filter((s) => s.type === selectedStrategy)[0];

  return result ? result.json_schema : undefined;
};

const _getDefaultStrategyConfig = (selectedStrategy, strategies) => {
  const result = strategies.filter((s) => s.type === selectedStrategy)[0];

  return result ? result.default_config : undefined;
};

const _getTimeBaseStrategyWithElasticLimit = (activeConfig, strategies) => {
  const timeBasedStrategy = _getDefaultStrategyConfig(TIME_BASED_ROTATION_STRATEGY, strategies);

  return { ...activeConfig, max_rotation_period: timeBasedStrategy?.max_rotation_period };
};

const _getStrategyConfig = (selectedStrategy, activeStrategy, activeConfig, strategies) => {
  if (selectedStrategy === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY) {
    return activeConfig;
  }

  if (activeStrategy === selectedStrategy) {
    // If the newly selected strategy is the current active strategy, we use the active configuration.
    return activeStrategy === TIME_BASED_ROTATION_STRATEGY ? _getTimeBaseStrategyWithElasticLimit(activeConfig, strategies) : activeConfig;
  }

  // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
  return _getDefaultStrategyConfig(selectedStrategy, strategies);
};

const _getConfigurationComponent = (selectedStrategy, pluginExports, strategies, strategy, config, onConfigUpdate) => {
  if (!selectedStrategy || selectedStrategy.length < 1) {
    return null;
  }

  const strategyPlugin = pluginExports.filter((exportedStrategy) => exportedStrategy.type === selectedStrategy)[0];

  if (!strategyPlugin) {
    return null;
  }

  const strategyConfig = _getStrategyConfig(selectedStrategy, strategy, config, strategies);
  const element = React.createElement(strategyPlugin.configComponent, {
    config: strategyConfig,
    jsonSchema: _getStrategyJsonSchema(selectedStrategy, strategies),
    updateConfig: onConfigUpdate,
  });

  return (<span key={strategy.type}>{element}</span>);
};

const IndexMaintenanceStrategiesConfiguration = ({
  title,
  name,
  description,
  selectPlaceholder,
  pluginExports,
  strategies,
  retentionStrategiesContext: { max_index_retention_period: maxRetentionPeriod },
  activeConfig: { strategy, config },
  getState,
}) => {
  const [newStrategy, setNewStrategy] = useState(strategy);
  const {
    setValues,
    values,
    values: {
      rotation_strategy: rotationStrategy,
      rotation_strategy_class: rotationStrategyClass,
      retention_strategy_class: retentionStrategyClass,
    },
  } = useFormikContext();

  const retentionIsNotNoop = retentionStrategyClass !== NOOP_RETENTION_STRATEGY;
  const isArchiveRetention = retentionStrategyClass !== ARCHIVE_RETENTION_STRATEGY;
  const shouldShowMaxRetentionWarning = maxRetentionPeriod && rotationStrategyClass === TIME_BASED_ROTATION_STRATEGY && retentionIsNotNoop;
  const isTimeBasedSizeOptimizing = rotationStrategyClass === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY;
  const shouldShowTimeBasedSizeOptimizing = isTimeBasedSizeOptimizing && name === 'retention';
  const helpText = isTimeBasedSizeOptimizing && name === 'rotation' ?
    'The Time Based Size Optimizing Rotation Strategy tries to rotate the index daily.' +
    ' It can however skip the rotation to achieve optimal sized indices by keeping the shard size between 20 and 50 GB.' +
    ' The optimization can delay the rotation within the range of the configured retention min/max lifetime.' +
    ' If an index is older than the range between min/max, it will be rotated regardless of its current size.'
    : null;

  const _onSelect = (selectedStrategy) => {
    if (!selectedStrategy || selectedStrategy.length < 1) {
      setNewStrategy(undefined);

      return;
    }

    const newConfig = _getStrategyConfig(selectedStrategy, strategy, config, strategies);

    setNewStrategy(selectedStrategy);
    setValues({ ...values, ...getState(selectedStrategy, newConfig) });
  };

  const _onConfigUpdate = useCallback((newConfig) => {
    const _addConfigType = (selectedStrategy, data) => {
      // The config object needs to have the "type" field set to the "default_config.type" to make the REST call work.
      const result = strategies.filter((s) => s.type === selectedStrategy)[0];
      const copy = data;

      if (result) {
        copy.type = result.default_config.type;
      }

      return copy;
    };

    const configuration = _addConfigType(newStrategy, newConfig);
    setValues({ ...values, ...getState(newStrategy, configuration) });
  }, [getState, newStrategy, setValues, strategies, values]);

  const _onIndexTimeSizeOptimizingUpdate = useCallback((newConfig) => {
    if (isTimeBasedSizeOptimizing) {
      setValues({ ...values, ...{ rotation_strategy_class: rotationStrategyClass, rotation_strategy: { ...newConfig, type: TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY_TYPE } } });
    }
  }, [isTimeBasedSizeOptimizing, rotationStrategyClass, setValues, values]);

  const _availableSelectOptions = () => {
    return pluginExports
      .filter((c) => strategies.find(({ type }) => type === c.type))
      .map((c) => {
        return { value: c.type, label: c.displayName };
      });
  };

  const _activeSelection = () => {
    return newStrategy;
  };

  function getDescription() {
    if (description) {
      return (
        <StyledAlert>
          <Icon name="info-circle" />{' '} {description}
        </StyledAlert>
      );
    }

    return null;
  }

  function getHelpText() {
    if (helpText) {
      return (
        <StyledAlert>
          <Icon name="info-circle" />{' '} {helpText}
        </StyledAlert>
      );
    }

    return null;
  }

  return (
    <span>
      <StyledH3>{title}</StyledH3>
      {getDescription()}
      {getHelpText()}
      {shouldShowMaxRetentionWarning && (
        <StyledAlert bsStyle="warning">
          <Icon name="exclamation-triangle" />{' '} The effective retention period value calculated from the
          <b>Rotation period</b> and the <b>max number of indices</b> should not be greater than the
          <b>Max retention period </b> of <b>{maxRetentionPeriod}</b> set by the Administrator.
        </StyledAlert>
      )}
      <Row>
        <Col md={12}>
          <Input id="strategy-select"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 label={selectPlaceholder}>
            <StyledSelect placeholder={selectPlaceholder}
                          options={_availableSelectOptions()}
                          matchProp="label"
                          value={_activeSelection()}
                          onChange={_onSelect}
                          clearable={false} />
          </Input>
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          {shouldShowTimeBasedSizeOptimizing && retentionIsNotNoop && _getConfigurationComponent(
            TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY,
            PluginStore.exports('indexRotationConfig'),
            [rotationStrategy],
            rotationStrategy,
            rotationStrategy,
            _onIndexTimeSizeOptimizingUpdate,
          )}
          {(!isTimeBasedSizeOptimizing
          || (name === 'retention' && (!retentionIsNotNoop || !isArchiveRetention)))
          && _getConfigurationComponent(
            _activeSelection(),
            pluginExports,
            strategies,
            strategy,
            config,
            _onConfigUpdate,
          )}
        </Col>
      </Row>
    </span>
  );
};

IndexMaintenanceStrategiesConfiguration.propTypes = {
  title: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  description: PropTypes.string,
  selectPlaceholder: PropTypes.string.isRequired,
  pluginExports: PropTypes.array.isRequired,
  strategies: PropTypes.array.isRequired,
  retentionStrategiesContext: PropTypes.shape({
    max_index_retention_period: PropTypes.string,
  }),
  activeConfig: PropTypes.object.isRequired,
  getState: PropTypes.func.isRequired,
};

IndexMaintenanceStrategiesConfiguration.defaultProps = {
  description: undefined,
  retentionStrategiesContext: {
    max_index_retention_period: undefined,
  },
};

export default IndexMaintenanceStrategiesConfiguration;
