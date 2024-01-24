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
import React, { useState } from 'react';
import moment from 'moment';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormikFormGroup, FormikInput, FormSubmit, Spinner, TimeUnitInput } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { Col, Row, Input } from 'components/bootstrap';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import 'components/indices/rotation';
import 'components/indices/retention';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { IndexSetPropType } from 'stores/indices/IndexSetsStore';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
} from 'components/indices/Types';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';
import useHistory from 'routing/useHistory';
import IndexSetProfileConfiguration from 'components/indices/IndexSetProfileConfiguration';

type Props = {
  cancelLink: string,
  create?: boolean,
  indexSet: IndexSet,
  onUpdate: (indexSet: IndexSet) => void,
  retentionStrategies: Array<any>,
  retentionStrategiesContext: RetentionStrategyContext,
  rotationStrategies: Array<any>,
  submitButtonText: string,
  submitLoadingText?: string,
};

type Unit = 'seconds' | 'minutes';

const StyledFormSubmit = styled(FormSubmit)`
  margin-left: 0;
`;

const _validateIndexPrefix = (value: string) => {
  let error: string;

  if (value?.length === 0) {
    error = 'Invalid index prefix: cannot be empty';
  } else if (value?.indexOf('_') === 0 || value?.indexOf('-') === 0 || value?.indexOf('+') === 0) {
    error = 'Invalid index prefix: must start with a letter or number';
  } else if (value?.toLocaleLowerCase() !== value) {
    error = 'Invalid index prefix: must be lower case';
  } else if (!value?.match(/^[a-z0-9][a-z0-9_\-+]*$/)) {
    error = 'Invalid index prefix: must only contain letters, numbers, \'_\', \'-\' and \'+\'';
  }

  return error;
};

const _getRotationConfigState = (strategy: string, data: RotationStrategyConfig) => ({
  rotation_strategy_class: strategy,
  rotation_strategy: data,
});

const _getRetentionConfigState = (strategy: string, data: RetentionStrategyConfig) => ({
  retention_strategy_class: strategy,
  retention_strategy: data,
});

type RotationStrategiesProps = {
  rotationStrategies: Array<any>,
  indexSetRotationStrategy: RotationStrategyConfig,
  indexSetRotationStrategyClass: string
}

const RotationStrategies = ({ rotationStrategies, indexSetRotationStrategy, indexSetRotationStrategyClass }: RotationStrategiesProps) => {
  if (!rotationStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                             name="rotation"
                                             description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                             selectPlaceholder="Select rotation strategy"
                                             pluginExports={PluginStore.exports('indexRotationConfig')}
                                             strategies={rotationStrategies}
                                             activeConfig={{
                                               config: indexSetRotationStrategy,
                                               strategy: indexSetRotationStrategyClass,
                                             }}
                                             getState={_getRotationConfigState} />
  );
};

type RetentionConfigProps = {
  retentionStrategies: Array<any>,
  retentionStrategiesContext: RetentionStrategyContext,
  indexSetRetentionStrategy: RetentionStrategyConfig,
  IndexSetRetentionStrategyClass: string
}

const RetentionConfig = ({ retentionStrategies, retentionStrategiesContext, indexSetRetentionStrategy, IndexSetRetentionStrategyClass }: RetentionConfigProps) => {
  if (!retentionStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                             name="retention"
                                             description="Graylog uses a retention strategy to clean up old indices."
                                             selectPlaceholder="Select retention strategy"
                                             pluginExports={PluginStore.exports('indexRetentionConfig')}
                                             strategies={retentionStrategies}
                                             retentionStrategiesContext={retentionStrategiesContext}
                                             activeConfig={{
                                               config: indexSetRetentionStrategy,
                                               strategy: IndexSetRetentionStrategyClass,
                                             }}
                                             getState={_getRetentionConfigState} />
  );
};

const ReadOnlyConfig = () => {
  const indexPrefixHelp = (
    <span>
      A <strong>unique</strong> prefix used in Elasticsearch indices belonging to this index set.
      The prefix must start with a letter or number, and can only contain letters, numbers, &apos;_&apos;, &apos;-&apos; and &apos;+&apos;.
    </span>
  );

  return (
    <span>
      <FormikFormGroup type="text"
                       label="Index prefix"
                       name="index_prefix"
                       help={indexPrefixHelp}
                       validate={_validateIndexPrefix}
                       required />
      <FormikFormGroup type="text"
                       label="Analyzer"
                       name="index_analyzer"
                       help="Elasticsearch analyzer for this index set."
                       required />
    </span>
  );
};

const IndexSetConfigurationForm = ({
  indexSet, onUpdate, rotationStrategies,
  retentionStrategies,
  create,
  cancelLink,
  retentionStrategiesContext,
  submitButtonText,
  submitLoadingText,
}: Props) => {
  const history = useHistory();
  const [indexSetState] = useState<IndexSet>(indexSet);
  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');

  const _saveConfiguration = (values: IndexSet) => onUpdate(values);

  const {
    rotation_strategy: indexSetRotationStrategy,
    rotation_strategy_class: indexSetRotationStrategyClass,
    retention_strategy: indexSetRetentionStrategy,
    retention_strategy_class: IndexSetRetentionStrategyClass,
  } = indexSet;
  const onCancel = () => history.push(cancelLink);

  return (
    <Row>
      <Col md={8}>
        <Formik onSubmit={_saveConfiguration}
                initialValues={indexSetState}>
          {({ isValid, setFieldValue, isSubmitting }) => (
            <IndexRetentionProvider>
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
                    {create && <ReadOnlyConfig />}
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
                      <Field name="field_type_refresh_interval">
                        {({ field: { name, value, onChange } }) => {
                          const _onFieldTypeRefreshIntervalChange = (intervalValue: number, unit: Unit) => {
                            onChange(name, moment.duration(intervalValue, unit).asMilliseconds());
                            setFieldValue(name, moment.duration(intervalValue, unit).asMilliseconds());
                            setFieldTypeRefreshIntervalUnit(unit);
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
                    </HideOnCloud>
                  </Col>
                </Row>
                {indexSetState.writable && <RotationStrategies rotationStrategies={rotationStrategies} indexSetRotationStrategy={indexSetRotationStrategy} indexSetRotationStrategyClass={indexSetRotationStrategyClass} />}
                {indexSetState.writable && <RetentionConfig retentionStrategies={retentionStrategies} retentionStrategiesContext={retentionStrategiesContext} indexSetRetentionStrategy={indexSetRetentionStrategy} IndexSetRetentionStrategyClass={IndexSetRetentionStrategyClass} />}
                <Field name="field_type_profile">
                  {({ field: { name, value } }) => (
                    <IndexSetProfileConfiguration value={value}
                                                  onChange={(profileId) => {
                                                    setFieldValue(name, profileId);
                                                  }}
                                                  name={name} />
                  )}
                </Field>
                <Row>
                  <Col md={9} mdOffset={3}>
                    <StyledFormSubmit disabledSubmit={!isValid}
                                      submitButtonText={submitButtonText}
                                      submitLoadingText={submitLoadingText}
                                      isSubmitting={isSubmitting}
                                      isAsyncSubmit
                                      displayCancel
                                      onCancel={onCancel} />
                  </Col>
                </Row>
              </Form>
            </IndexRetentionProvider>

          )}
        </Formik>
      </Col>
    </Row>
  );
};

IndexSetConfigurationForm.propTypes = {
  indexSet: IndexSetPropType.isRequired,
  rotationStrategies: PropTypes.array.isRequired,
  retentionStrategies: PropTypes.array.isRequired,
  retentionStrategiesContext: PropTypes.shape({
    max_index_retention_period: PropTypes.string,
  }).isRequired,
  create: PropTypes.bool,
  onUpdate: PropTypes.func.isRequired,
  cancelLink: PropTypes.string.isRequired,
  submitButtonText: PropTypes.string.isRequired,
  submitLoadingText: PropTypes.string.isRequired,
};

IndexSetConfigurationForm.defaultProps = {
  create: false,
};

export default IndexSetConfigurationForm;
