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
import { useFormikContext, Field } from 'formik';
import styled from 'styled-components';

import { Alert, Input } from 'components/bootstrap';
import { Icon, Select } from 'components/common';

const TIME_BASED_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy';
const NOOP_RETENTION_STRATEGY = 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy';

const StyledH3 = styled.h3`
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
      rotation_strategy_class: rotationStrategyClass,
      retention_strategy_class: retentionStrategyClass,
    },
  } = useFormikContext();

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

  const retentionIsNotNoop = retentionStrategyClass !== NOOP_RETENTION_STRATEGY;
  const shouldShowMaxRetentionWarning = maxRetentionPeriod && rotationStrategyClass === TIME_BASED_ROTATION_STRATEGY && retentionIsNotNoop;

  function getDescription() {
    if (description) {
      return <StyledAlert>
        <Icon name="info-circle" />{' '} {description}
      </StyledAlert>;
    }

    return null;
  }

  return (
    <>
    <Field name="settings.aws_region">
      {({ meta }) => (
        <Input id="strategy-select"

               label={selectPlaceholder}>
          <Select placeholder={selectPlaceholder}
                  options={_availableSelectOptions()}
                  matchProp="label"
                  value={_activeSelection()}
                  onChange={_onSelect} />
        </Input>
      )}
    </Field>
      {_getConfigurationComponent(_activeSelection(), pluginExports, strategies, strategy, config, _onConfigUpdate)}
    </>
  );
};

IndexMaintenanceStrategiesConfiguration.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
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
  retentionStrategiesContext: {
    max_index_retention_period: undefined,
  },
};

export default IndexMaintenanceStrategiesConfiguration;
