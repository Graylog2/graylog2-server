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
import * as React from 'react';
import { useCallback, useState, useEffect } from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type {
  RetentionStrategyConfig,
  RotationStrategy,
  RetentionStrategy,
  TimeBasedRotationStrategyConfig,
  JsonSchema,
  StrategyConfig,
  Strategy,
  Strategies,
} from 'components/indices/Types';
import type { SystemConfigurationComponentProps } from 'views/types';
import {
  ARCHIVE_RETENTION_STRATEGY,
  NOOP_RETENTION_STRATEGY,
  RETENTION,
  TIME_BASED_ROTATION_STRATEGY,
  TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY,
  TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY_TYPE,
} from 'stores/indices/IndicesStore';
import { Alert, Col, Input, Row } from 'components/bootstrap';
import { Select } from 'components/common';
import { useIndexRetention } from 'components/indices/contexts/IndexRetentionContext';

type IndexMaintenanceStrategiesFormValues = {
  rotation_strategy?: RotationStrategy,
  rotation_strategy_class?: string,
  retention_strategy_class?: string,
  retention_strategy?: RetentionStrategy,
}

interface ConfigComponentProps extends SystemConfigurationComponentProps {
  config: StrategyConfig,
  jsonSchema: JsonSchema,
  updateConfig: (update: StrategyConfig) => void
  useMaxNumberOfIndices?: () => [
    number | undefined,
    React.Dispatch<React.SetStateAction<number>>
  ],
}

type Props = {
  title: string,
  name: string,
  description?: string,
  selectPlaceholder: string,
  label: string,
  pluginExports: Array<{
    type: string,
    displayName: string,
    configComponent: React.ComponentType<ConfigComponentProps>
  }>,
  strategies: Strategies,
  retentionStrategiesContext?: {
    max_index_retention_period?: string,
  },
  activeConfig: {
    strategy?: string,
    config?: StrategyConfig,
  },
  getState: (strategy: string, data: StrategyConfig) => {
    rotation_strategy_config?: StrategyConfig,
    rotation_strategy_class?: string,
    retention_strategy_config?: StrategyConfig,
    retention_strategy_class?: string,
  },
}

const hasRetentionConfigField = (configData: StrategyConfig, field: string): configData is RetentionStrategyConfig => field in configData;

const StyledH3 = styled.h3`
  margin-bottom: 10px;
`;
const StyledSelect = styled(Select)`
  margin-bottom: 10px;
`;
const StyledAlert = styled(Alert)`
  overflow: auto;
`;

const getStrategyJsonSchema = (selectedStrategy: string, strategies: Strategies) : JsonSchema | undefined => {
  const result = strategies.filter((s) => s.type === selectedStrategy)[0];

  return result ? result.json_schema : undefined;
};

const getDefaultStrategyConfig = (selectedStrategy: string, strategies: Strategies) : StrategyConfig | undefined => {
  const result = strategies.filter((s) => s.type === selectedStrategy)[0];

  return result ? result.default_config : undefined;
};

const getTimeBaseStrategyWithElasticLimit = (activeConfig: TimeBasedRotationStrategyConfig, strategies: Strategies) : StrategyConfig => {
  const timeBasedStrategy = getDefaultStrategyConfig(TIME_BASED_ROTATION_STRATEGY, strategies) as TimeBasedRotationStrategyConfig;

  return { ...activeConfig, max_rotation_period: timeBasedStrategy?.max_rotation_period };
};

const getStrategyConfig = (configTypeName: string, selectedStrategy: string, activeStrategy: string, activeConfig: StrategyConfig, strategies: Strategies) : StrategyConfig | undefined => {
  if (selectedStrategy === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY && configTypeName === 'retention') {
    return activeConfig;
  }

  if (activeStrategy === selectedStrategy) {
    // If the newly selected strategy is the current active strategy, we use the active configuration.
    return activeStrategy === TIME_BASED_ROTATION_STRATEGY ? getTimeBaseStrategyWithElasticLimit(activeConfig as TimeBasedRotationStrategyConfig, strategies) : activeConfig;
  }

  // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
  return getDefaultStrategyConfig(selectedStrategy, strategies);
};

const getConfigurationComponent = (
  configTypeName: string,
  selectedStrategy: string,
  pluginExports: Array<{
    type: string,
    displayName: string,
    configComponent: React.ComponentType,
  }>,
  strategies: Strategies,
  strategy: Strategy | string,
  config: StrategyConfig,
  onConfigUpdate: (update: StrategyConfig) => void,
  useMaxNumberOfIndices: () => [
    number | undefined,
    React.Dispatch<React.SetStateAction<number>>
  ]) => {
  if (!selectedStrategy || selectedStrategy.length < 1) {
    return null;
  }

  const strategyPlugin = pluginExports.filter((exportedStrategy) => exportedStrategy.type === selectedStrategy)[0];

  if (!strategyPlugin) {
    return null;
  }

  const strategyType = typeof strategy === 'string' ? strategy : strategy?.type;

  const strategyConfig = getStrategyConfig(configTypeName, selectedStrategy, strategyType, config, strategies);

  let componentProps : ConfigComponentProps = {
    config: strategyConfig,
    jsonSchema: getStrategyJsonSchema(selectedStrategy, strategies),
    updateConfig: onConfigUpdate,
  };

  if (selectedStrategy === ARCHIVE_RETENTION_STRATEGY) {
    componentProps = { ...componentProps, useMaxNumberOfIndices };
  }

  const element = React.createElement(strategyPlugin.configComponent as React.ComponentType<ConfigComponentProps>, componentProps);

  return (<div key={strategyType}>{element}</div>);
};

const IndexMaintenanceStrategiesConfiguration = ({
  title,
  name,
  description,
  selectPlaceholder,
  label,
  pluginExports,
  strategies,
  retentionStrategiesContext: { max_index_retention_period: maxRetentionPeriod } = {
    max_index_retention_period: undefined,
  },
  activeConfig: { strategy, config },
  getState,
} : Props) => {
  const [newStrategy, setNewStrategy] = useState<string | undefined>(strategy);
  const {
    setValues,
    values,
    values: {
      rotation_strategy: rotationStrategy,
      rotation_strategy_class: rotationStrategyClass,
      retention_strategy_class: retentionStrategyClass,
    },
    errors,
  } = useFormikContext<IndexMaintenanceStrategiesFormValues>();

  const [maxNumberOfIndices, setMaxNumberOfIndices] = useIndexRetention().useMaxNumberOfIndices;

  useEffect(() => {
    if (config && hasRetentionConfigField(config, 'max_number_of_indices')) {
      setMaxNumberOfIndices(config.max_number_of_indices);
    }
  }, [config, setMaxNumberOfIndices]);

  const retentionIsNotNoop = retentionStrategyClass !== NOOP_RETENTION_STRATEGY;
  const isArchiveRetention = retentionStrategyClass !== ARCHIVE_RETENTION_STRATEGY;
  const shouldShowMaxRetentionWarning = maxRetentionPeriod && rotationStrategyClass === TIME_BASED_ROTATION_STRATEGY && retentionIsNotNoop;
  const isTimeBasedSizeOptimizing = rotationStrategyClass === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY;
  const shouldShowTimeBasedSizeOptimizingForm = isTimeBasedSizeOptimizing && name === 'retention' && retentionIsNotNoop;
  const shouldShowNormalRetentionForm = (!isTimeBasedSizeOptimizing || (name === 'retention' && (!retentionIsNotNoop || !isArchiveRetention)));
  const helpText = isTimeBasedSizeOptimizing && name === 'rotation'
    ? 'The Time Based Size Optimizing Rotation Strategy tries to rotate the index daily.'
    + ' It can however skip the rotation to achieve optimal sized indices by keeping the shard size within an acceptable range.'
    + ' The optimization can delay the rotation within the range of the configured retention min/max lifetime.'
    + ' If an index is older than the range between min/max, it will be rotated regardless of its current size.'
    : null;

  const _onSelect = (selectedStrategy) => {
    if (!selectedStrategy || selectedStrategy.length < 1) {
      setNewStrategy(undefined);

      return;
    }

    const newConfig = getStrategyConfig(name, selectedStrategy, strategy, config, strategies);
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

  const getAvailableSelectOptions = () => {
    const availableStrategies = pluginExports
      .filter((pluginOptions) => strategies.find(({ type }) => type === pluginOptions.type));

    const isSelectedItemInList = availableStrategies.filter((availableStrategy) => availableStrategy.type === newStrategy).length > 0;

    if (!isSelectedItemInList) {
      const selectedItemStrategy = pluginExports.find((pluginOptions) => pluginOptions.type === newStrategy);

      if (selectedItemStrategy) {
        return [...availableStrategies, selectedItemStrategy].map((pluginOptions) => ({ value: pluginOptions.type, label: pluginOptions.displayName }));
      }

      return availableStrategies.map((pluginOptions) => ({ value: pluginOptions.type, label: pluginOptions.displayName }));
    }

    return availableStrategies
      .map((c) => ({ value: c.type, label: c.displayName }));
  };

  const getDisplayName = () => pluginExports.find((pluginOptions) => pluginOptions.type === newStrategy).displayName;

  const getActiveSelection = () => newStrategy;

  const shouldShowInvalidRetentionWarning = () => (
    !!newStrategy && name === RETENTION && !getStrategyJsonSchema(getActiveSelection(), strategies)
  );

  return (
    <div>
      <StyledH3>{title}</StyledH3>
      {description && (
        <StyledAlert>
          {description}
        </StyledAlert>
      )}
      {helpText && (
        <StyledAlert>
          {helpText}
        </StyledAlert>
      )}
      {shouldShowMaxRetentionWarning && (
        <StyledAlert bsStyle="warning">
          The effective retention period value calculated from the
          <b>Rotation period</b> and the <b>max number of indices</b> should not be greater than the
          <b>Max retention period </b> of <b>{maxRetentionPeriod}</b> set by the Administrator.
        </StyledAlert>
      )}
      {shouldShowInvalidRetentionWarning() && (
        <StyledAlert bsStyle="danger">
          {getDisplayName()} strategy was deactivated.
          Please configure a valid retention strategy.
        </StyledAlert>
      )}
      <Row>
        <Col md={12}>
          <Input id="strategy-select"
                 error={errors[`${name}_strategy_class`]}
                 name={`${name}_strategy_class`}
                 label={label}>
            <StyledSelect placeholder={selectPlaceholder}
                          options={getAvailableSelectOptions()}
                          matchProp="label"
                          value={getActiveSelection()}
                          onChange={_onSelect}
                          clearable={false} />
          </Input>
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          {shouldShowTimeBasedSizeOptimizingForm && getConfigurationComponent(
            name,
            TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY,
            PluginStore.exports('indexRotationConfig'),
            [rotationStrategy],
            rotationStrategy,
            rotationStrategy,
            _onIndexTimeSizeOptimizingUpdate,
            () => [maxNumberOfIndices, setMaxNumberOfIndices],
          )}
          {shouldShowNormalRetentionForm && getConfigurationComponent(
            name,
            getActiveSelection(),
            pluginExports,
            strategies,
            strategy,
            config,
            _onConfigUpdate,
            () => [maxNumberOfIndices, setMaxNumberOfIndices],
          )}
        </Col>
      </Row>
    </div>
  );
};

export default IndexMaintenanceStrategiesConfiguration;
