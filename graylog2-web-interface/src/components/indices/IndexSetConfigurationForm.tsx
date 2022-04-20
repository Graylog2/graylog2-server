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
import moment from 'moment';
import lodash from 'lodash';
import { Formik, Form, Field } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormikFormGroup, FormikInput, Spinner, TimeUnitInput } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { LinkContainer } from 'components/common/router';
import { Col, Row, Button, Input } from 'components/bootstrap';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import 'components/indices/rotation';
import 'components/indices/retention';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

import type { RetentionStrategyContext } from './Types';

type Props = {
  indexSet: IndexSet,
  rotationStrategies: Array<any>,
  retentionStrategies: Array<any>,
  retentionStrategiesContext: RetentionStrategyContext,
  create: boolean,
  onUpdate: (indexSet: IndexSet) => void,
  cancelLink: string,
};

type Unit = 'seconds' | 'minutes';

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
    retentionStrategiesContext: PropTypes.shape({
      max_index_retention_period: PropTypes.string,
    }).isRequired,
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
      fieldTypeRefreshIntervalUnit: 'seconds',
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

  _validateIndexPrefix = (event: React.ChangeEvent<HTMLInputElement>) => {
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
      } else if (value.toLocaleLowerCase() !== value) {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: must be lower case';
      } else {
        nextValidationErrors[event.target.name] = 'Invalid index prefix: must only contain letters, numbers, \'_\', \'-\' and \'+\'';
      }

      this.setState({ validationErrors: nextValidationErrors });
    }

    this._onInputChange(event);
  };

  _onInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this._updateConfig(event.target.name, event.target.value);
  };

  _onDisableOptimizationClick = (event: React.ChangeEvent<HTMLInputElement>) => {
    this._updateConfig(event.target.name, event.target.checked);
  };

  // _saveConfiguration = (event: React.FormEvent<HTMLFormElement>) => {
  _saveConfiguration = (values) => {
    // event.preventDefault();
    const { indexSet, validationErrors } = this.state;
    const { onUpdate } = this.props;
    const invalidFields = Object.keys(validationErrors);

    if (invalidFields.length !== 0) {
      document.getElementsByName(invalidFields[0])[0].focus();

      return;
    }

    onUpdate(values);
  };

  _updateRotationConfigState = (strategy: string, data: string) => {
    this._updateConfig('rotation_strategy_class', strategy);
    this._updateConfig('rotation_strategy', data);
  };

  _updateRetentionConfigState = (strategy: string, data: string) => {
    this._updateConfig('retention_strategy_class', strategy);
    this._updateConfig('retention_strategy', data);
  };

  _getRotationConfigState = (strategy: string, data: string) => {
    return { rotation_strategy_class: strategy, rotation_strategy: data };
  };

  _getRetentionConfigState = (strategy: string, data: string) => {
    return { retention_strategy_class: strategy, retention_strategy: data };
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
        rotation_strategy: indexSetRotationStrategy,
        rotation_strategy_class: indexSetRotationStrategyClass,
        retention_strategy: indexSetRetentionStrategy,
        retention_strategy_class: IndexSetRetentionStrategyClass,
      },
      retentionStrategiesContext,
    } = this.props;
    let rotationConfig;

    if (rotationStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: indexSetRotationStrategy,
        strategy: indexSetRotationStrategyClass,
      };

      rotationConfig = (
        <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                 key="rotation"
                                                 description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                                 selectPlaceholder="Select rotation strategy"
                                                 pluginExports={PluginStore.exports('indexRotationConfig')}
                                                 strategies={rotationStrategies}
                                                 activeConfig={activeConfig}
                                                 getState={this._getRotationConfigState} />
      );
    } else {
      rotationConfig = (<Spinner />);
    }

    let retentionConfig;

    if (retentionStrategies) {
      // The component expects a different structure - legacy
      const activeConfig = {
        config: indexSetRetentionStrategy,
        strategy: IndexSetRetentionStrategyClass,
      };

      retentionConfig = (
        <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                 key="retention"
                                                 description="Graylog uses a retention strategy to clean up old indices."
                                                 selectPlaceholder="Select retention strategy"
                                                 pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                 strategies={retentionStrategies}
                                                 retentionStrategiesContext={retentionStrategiesContext}
                                                 activeConfig={activeConfig}
                                                 getState={this._getRetentionConfigState} />
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
          <FormikFormGroup type="text"
                           label="Index prefix"
                           name="index_prefix"
                           help={validationErrors.index_prefix ? validationErrors.index_prefix : indexPrefixHelp}
                           required />
          <FormikFormGroup type="text"
                           label="Analyzer"
                           name="index_analyzer"
                           onChange={this._onInputChange}
                           help="Elasticsearch analyzer for this index set."
                           required />
        </span>
      );
    }

    return (
      <Row>
        <Col md={8}>
          {/* <form className="form" onSubmit={this._saveConfiguration}> */}
          <Formik onSubmit={this._saveConfiguration}
                  initialValues={indexSet}>
            {({ isSubmitting, isValid, setFieldValue }) => (
              <Form>
                <Row>
                  <Col md={12}>
                    <FormikFormGroup type="text"
                                     label="Title"
                                     name="title"
                                     help="Descriptive name of the index set."
                                     required />
                    <FormikFormGroup type="text"
                                     label="Description"
                                     name="description"
                                     help="Add a description of this index set."
                                     required />
                    {readOnlyconfig}
                    <HideOnCloud>
                      <FormikFormGroup type="number"
                                       label="Index shards"
                                       name="shards"
                                       help="Number of Elasticsearch shards used per index in this index set."
                                       required />
                      <FormikFormGroup type="number"
                                       label="Index replicas"
                                       name="replicas"
                                       help="Number of Elasticsearch replicas used per index in this index set."
                                       required />
                      <FormikFormGroup type="number"
                                       label="Max. number of segments"
                                       name="index_optimization_max_num_segments"
                                       minLength={1}
                                       help="Maximum number of segments per Elasticsearch index after optimization (force merge)."
                                       required />
                      <Input id="roles-selector-input"
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9"
                             label="Index optimization after rotation">
                        <FormikInput type="checkbox"
                                     id="index_optimization_disabled"
                                     label="Disable index optimization after rotation"
                                     name="index_optimization_disabled"
                                     help="Disable Elasticsearch index optimization (force merge) after rotation." />
                      </Input>
                    </HideOnCloud>
                    <Field name="field_type_refresh_interval">
                      {({ field: { name, value, onChange } }) => {
                        const _onFieldTypeRefreshIntervalChange = (intervalValue: number, unit: Unit) => {
                          onChange(name, moment.duration(intervalValue, unit).asMilliseconds());
                          setFieldValue(name, moment.duration(intervalValue, unit).asMilliseconds());
                          this.setState({ fieldTypeRefreshIntervalUnit: unit });
                        };

                        return (
                          <Input id="roles-selector-input"
                                 labelClassName="col-sm-3"
                                 wrapperClassName="col-sm-9"
                                 label="Field type refresh interval">
                            <TimeUnitInput id="field-type-refresh-interval"
                                           type="number"
                                           help="How often the field type information for the active write index will be updated."
                                           value={moment.duration(value, 'milliseconds').as(fieldTypeRefreshIntervalUnit)}
                                           unit={fieldTypeRefreshIntervalUnit.toUpperCase()}
                                           units={['SECONDS', 'MINUTES']}
                                           required
                                           update={_onFieldTypeRefreshIntervalChange} />
                          </Input>
                        );
                      }}
                    </Field>
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

              </Form>
            )}
          </Formik>
        </Col>
      </Row>
    );
  }
}

export default IndexSetConfigurationForm;
