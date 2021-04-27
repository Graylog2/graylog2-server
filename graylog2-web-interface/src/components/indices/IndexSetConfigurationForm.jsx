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
// @flow strict
import PropTypes from 'prop-types';
import React from 'react';
import moment from 'moment';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import HideOnCloud from 'util/conditional/HideOnCloud'; // Load rotation plugin UI plugins from core.
import { LinkContainer } from 'components/graylog/router';
import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Spinner, TimeUnitInput } from 'components/common';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import {} from 'components/indices/rotation'; // Load rotation plugin UI plugins from core.
import {} from 'components/indices/retention';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

type Props = {
  indexSet: IndexSet,
  rotationStrategies: any,
  retentionStrategies: any,
  create: boolean,
  onUpdate: (indexSet: IndexSet) => void,
  cancelLink: string,
};

type Unit = 'SECONDS' | 'MINUTES';

type State = {
  indexSet: IndexSet,
  fieldTypeRefreshIntervalUnit: Unit,
  validationErrors: {
    [key: string]: string,
  },
};

class IndexSetConfigurationForm extends React.Component<Props, State> {
  static propTypes = {
    indexSet: PropTypes.object.isRequired,
    rotationStrategies: PropTypes.array.isRequired,
    retentionStrategies: PropTypes.array.isRequired,
    create: PropTypes.bool,
    onUpdate: PropTypes.func.isRequired,
    cancelLink: PropTypes.string.isRequired,
  };

  static defaultProps = {
    create: false,
  };

  constructor(props: Props) {
    super(props);
    const { indexSet } = this.props;

    this.state = {
      indexSet: indexSet,
      fieldTypeRefreshIntervalUnit: 'SECONDS',
      validationErrors: {},
    };
  }

  _updateConfig = (fieldName: string, value: (string | boolean | number)) => {
    // Use `setState()` with updater function so consecutive calls to `_updateConfig()` always refer to the state
    // at the time the change is applied, resulting in all different keys of the object being updated.
    this.setState((state) => {
      const config = lodash.cloneDeep(state.indexSet);

      config[fieldName] = value;

      return { indexSet: config };
    });
  };

  _validateIndexPrefix = (event: SyntheticInputEvent<HTMLInputElement>) => {
    const { value } = event.target;
    const { validationErrors } = this.state;

    if (value.match(/^[a-z0-9][a-z0-9_\-+]*$/)) {
      if (validationErrors[event.target.name]) {
        const nextValidationErrors = { ...validationErrors };

        delete nextValidationErrors[event.target.name];
        this.setState({ validationErrors: nextValidationErrors });
      }
    } else {
      const nextValidationErrors = { ...validationErrors };

      if (value.length === 0) {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: cannot be empty';
      } else if (value.indexOf('_') === 0 || value.indexOf('-') === 0 || value.indexOf('+') === 0) {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: must start with a letter or number';
      } else if (value.toLowerCase() !== value) {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: must be lower case';
      } else {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: must only contain letters, numbers, \'_\', \'-\' and \'+\'';
      }

      this.setState({ validationErrors: nextValidationErrors });
    }

    this._onInputChange(event);
  };

  _onInputChange = (event: SyntheticInputEvent<HTMLInputElement>) => {
    this._updateConfig(event.target.name, event.target.value);
  };

  _onDisableOptimizationClick = (event: SyntheticInputEvent<HTMLInputElement>) => {
    this._updateConfig(event.target.name, event.target.checked);
  };

  _saveConfiguration = (event: SyntheticInputEvent<HTMLInputElement>) => {
    event.preventDefault();
    const { indexSet, validationErrors } = this.state;
    const { onUpdate } = this.props;
    const invalidFields = Object.keys(validationErrors);

    if (invalidFields.length !== 0) {
      document.getElementsByName(invalidFields[0])[0].focus();

      return;
    }

    onUpdate(indexSet);
  };

  _updateRotationConfigState = (strategy: string, data: string) => {
    this._updateConfig('rotation_strategy_class', strategy);
    this._updateConfig('rotation_strategy', data);
  };

  _updateRetentionConfigState = (strategy: string, data: string) => {
    this._updateConfig('retention_strategy_class', strategy);
    this._updateConfig('retention_strategy', data);
  };

  _onFieldTypeRefreshIntervalChange = (value: number, unit: Unit) => {
    this._updateConfig('field_type_refresh_interval', moment.duration(value, unit).asMilliseconds());
    this.setState({ fieldTypeRefreshIntervalUnit: unit });
  };

  render() {
    const { indexSet, fieldTypeRefreshIntervalUnit } = this.state;
    const { validationErrors } = this.state;
    const {
      rotationStrategies,
      retentionStrategies,
      create,
      cancelLink,
      indexSet: {
        rotation_strategy: rotationStrategy,
        rotation_strategy_class: rotationStrategyClass,
        retention_strategy: retentionStrategy,
        retention_strategy_class: retentionStrategyClass,
      },
    } = this.props;
    let rotationConfig;

    if (rotationStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: rotationStrategy,
        strategy: rotationStrategyClass,
      };

      rotationConfig = (
        <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                 description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                                 selectPlaceholder="Select rotation strategy"
                                                 pluginExports={PluginStore.exports('indexRotationConfig')}
                                                 strategies={rotationStrategies}
                                                 activeConfig={activeConfig}
                                                 updateState={this._updateRotationConfigState} />
      );
    } else {
      rotationConfig = (<Spinner />);
    }

    let retentionConfig;

    if (retentionStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: retentionStrategy,
        strategy: retentionStrategyClass,
      };

      retentionConfig = (
        <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                 description="Graylog uses a retention strategy to clean up old indices."
                                                 selectPlaceholder="Select retention strategy"
                                                 pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                 strategies={retentionStrategies}
                                                 activeConfig={activeConfig}
                                                 updateState={this._updateRetentionConfigState} />
      );
    } else {
      retentionConfig = (<Spinner />);
    }

    let readOnlyconfig;

    if (create) {
      const indexPrefixHelp = (
        <span>
          A <strong>unique</strong> prefix used in Elasticsearch indices belonging to this index set.
          The prefix must start with a letter or number, and can only contain letters, numbers, &apos;_&apos;, &apos;-&apos; and &apos;+&apos;.
        </span>
      );

      readOnlyconfig = (
        <span>
          <Input type="text"
                 id="index-set-index-prefix"
                 label="Index prefix"
                 name="index_prefix"
                 onChange={this._validateIndexPrefix}
                 value={indexSet.index_prefix}
                 help={validationErrors.index_prefix ? validationErrors.index_prefix : indexPrefixHelp}
                 bsStyle={validationErrors.index_prefix ? 'error' : null}
                 required />
          <Input type="text"
                 id="index-set-index-analyzer"
                 label="Analyzer"
                 name="index_analyzer"
                 onChange={this._onInputChange}
                 value={indexSet.index_analyzer}
                 help="Elasticsearch analyzer for this index set."
                 required />
        </span>
      );
    }

    return (
      <Row>
        <Col md={8}>
          <form className="form" onSubmit={this._saveConfiguration}>
            <Row>
              <Col md={12}>
                <Input type="text"
                       id="index-set-title"
                       label="Title"
                       name="title"
                       onChange={this._onInputChange}
                       value={indexSet.title}
                       help="Descriptive name of the index set."
                       autoFocus
                       required />
                <Input type="text"
                       id="index-set-description"
                       label="Description"
                       name="description"
                       onChange={this._onInputChange}
                       value={indexSet.description}
                       help="Add a description of this index set."
                       required />
                {readOnlyconfig}
                <HideOnCloud>
                  <Input type="number"
                         id="index-set-shards"
                         label="Index shards"
                         name="shards"
                         onChange={this._onInputChange}
                         value={indexSet.shards}
                         help="Number of Elasticsearch shards used per index in this index set."
                         required />
                  <Input type="number"
                         id="index-set-replicas"
                         label="Index replicas"
                         name="replicas"
                         onChange={this._onInputChange}
                         value={indexSet.replicas}
                         help="Number of Elasticsearch replicas used per index in this index set."
                         required />
                  <Input type="number"
                         id="index-set-max-num-segments"
                         label="Max. number of segments"
                         name="index_optimization_max_num_segments"
                         min="1"
                         onChange={this._onInputChange}
                         value={indexSet.index_optimization_max_num_segments}
                         help="Maximum number of segments per Elasticsearch index after optimization (force merge)."
                         required />
                  <Input type="checkbox"
                         id="index-set-disable-optimization"
                         label="Disable index optimization after rotation"
                         name="index_optimization_disabled"
                         onChange={this._onDisableOptimizationClick}
                         checked={indexSet.index_optimization_disabled}
                         help="Disable Elasticsearch index optimization (force merge) after rotation." />
                </HideOnCloud>
                <TimeUnitInput id="field-type-refresh-interval"
                               label="Field type refresh interval"
                               help="How often the field type information for the active write index will be updated."
                               value={moment.duration(indexSet.field_type_refresh_interval, 'milliseconds').as(fieldTypeRefreshIntervalUnit)}
                               unit={fieldTypeRefreshIntervalUnit}
                               units={['SECONDS', 'MINUTES']}
                               required
                               update={this._onFieldTypeRefreshIntervalChange} />
              </Col>
            </Row>
            <Row>
              <Col md={12}>
                {indexSet.writable && rotationConfig}
              </Col>
            </Row>
            <Row>
              <Col md={12}>
                {indexSet.writable && retentionConfig}
              </Col>
            </Row>

            <Row>
              <Col md={12}>
                <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save</Button>
                <LinkContainer to={cancelLink}>
                  <Button bsStyle="default">Cancel</Button>
                </LinkContainer>
              </Col>
            </Row>
          </form>
        </Col>
      </Row>
    );
  }
}

export default IndexSetConfigurationForm;
