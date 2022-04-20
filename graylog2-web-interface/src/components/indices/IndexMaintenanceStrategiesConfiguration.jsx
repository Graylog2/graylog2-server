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
import { useState } from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';

import { Input, Alert } from 'components/bootstrap';
import { Select, Icon } from 'components/common';

const TIME_BASED_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy';

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

const IndexMaintenanceStrategiesConfiguration = ({ title, description, selectPlaceholder, pluginExports, strategies, retentionStrategiesContext, activeConfig: { strategy, config }, getState }) => {
  const [activeStrategy, setActiveStrategy] = useState(strategy);
  const [activeConfig, setActiveConfig] = useState(config);
  const [newStrategy, setNewStrategy] = useState(strategy);
  const { setValues, values } = useFormikContext();

  const _getDefaultStrategyConfig = (selectedStrategy) => {
    const result = strategies.filter((s) => s.type === selectedStrategy)[0];

    return result ? result.default_config : undefined;
  };

  const _getStrategyJsonSchema = (selectedStrategy) => {
    const result = strategies.filter((s) => s.type === selectedStrategy)[0];

    return result ? result.json_schema : undefined;
  };

  const _getTimeBaseStrategyWithElasticLimit = () => {
    const timeBasedStrategy = _getDefaultStrategyConfig(TIME_BASED_ROTATION_STRATEGY);

    return { ...activeConfig, max_rotation_period: timeBasedStrategy?.max_rotation_period };
  };

  const _getStrategyConfig = (selectedStrategy) => {
    if (activeStrategy === selectedStrategy) {
      // If the newly selected strategy is the current active strategy, we use the active configuration.
      return activeStrategy === TIME_BASED_ROTATION_STRATEGY ? _getTimeBaseStrategyWithElasticLimit() : activeConfig;
    }

    // If the newly selected strategy is not the current active strategy, we use the selected strategy's default config.
    return _getDefaultStrategyConfig(selectedStrategy);
  };

  const _onSelect = (selectedStrategy) => {
    if (!selectedStrategy || selectedStrategy.length < 1) {
      setNewStrategy(undefined);

      return;
    }

    const newConfig = _getStrategyConfig(selectedStrategy);

    setNewStrategy(selectedStrategy);
    setValues({ ...values, ...getState(selectedStrategy, newConfig) });
  };

  const _addConfigType = (selectedStrategy, data) => {
    // The config object needs to have the "type" field set to the "default_config.type" to make the REST call work.
    const result = strategies.filter((s) => s.type === selectedStrategy)[0];
    const copy = data;

    if (result) {
      copy.type = result.default_config.type;
    }

    return copy;
  };

  const _onConfigUpdate = (newConfig) => {
    const configuration = _addConfigType(newStrategy, newConfig);

    setValues({ ...values, ...getState(newStrategy, configuration) });
  };

  const _availableSelectOptions = () => {
    return pluginExports
      .filter((c) => strategies.find(({ type }) => type === c.type))
      .map((c) => {
        return { value: c.type, label: c.displayName };
      });
  };

  const _getConfigurationComponent = (selectedStrategy) => {
    if (!selectedStrategy || selectedStrategy.length < 1) {
      return null;
    }

    const strategy = pluginExports.filter((exportedStrategy) => exportedStrategy.type === selectedStrategy)[0];

    if (!strategy) {
      return null;
    }

    const strategyConfig = _getStrategyConfig(selectedStrategy);
    const element = React.createElement(strategy.configComponent, {
      config: strategyConfig,
      jsonSchema: _getStrategyJsonSchema(selectedStrategy),
      updateConfig: _onConfigUpdate,
    });

    return (<span key={strategy.type}>{element}</span>);
  };

  const _activeSelection = () => {
    return newStrategy;
  };

  return (
    <span>
      <StyledH3>{title}</StyledH3>
      <StyledAlert bsStyle="info">
        <Icon name="info-circle" />{' '} {description}
      </StyledAlert>
      <Input id="strategy-select"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label={selectPlaceholder}>
        <StyledSelect placeholder={selectPlaceholder}
                      options={_availableSelectOptions()}
                      matchProp="label"
                      value={_activeSelection()}
                      onChange={_onSelect} />
      </Input>
      {_getConfigurationComponent(_activeSelection())}
    </span>
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
