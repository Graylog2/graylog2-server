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
import React, { useState } from 'react';
import moment from 'moment';
import { Formik, Form, Field } from 'formik';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AppConfig from 'util/AppConfig';
import { FormikFormGroup, FormikInput, FormSubmit, Spinner, TimeUnitInput } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { Col, Row, Input, SegmentedControl } from 'components/bootstrap';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import 'components/indices/rotation';
import 'components/indices/retention';
import { DataTieringConfiguration, DataTieringVisualisation, prepareDataTieringConfig } from 'components/indices/data-tiering';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import withHistory from 'routing/withHistory';
import type { HistoryFunction } from 'routing/useHistory';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
  Strategies,
} from 'components/indices/Types';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';

type Props = {
  cancelLink: string,
  create?: boolean,
  history: HistoryFunction,
  indexSet: IndexSet,
  onUpdate: (indexSet: IndexSet) => void,
  retentionStrategies: Strategies,
  retentionStrategiesContext: RetentionStrategyContext,
  rotationStrategies: Strategies,
  submitButtonText: string,
  submitLoadingText?: string,
};

type Unit = 'seconds' | 'minutes';

const StyledFormSubmit = styled(FormSubmit)`
  margin-left: 0;
`;

const ConfigSegmentsTitle = styled.h2(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const ConfigSegment = styled.div(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
  margin-top: ${theme.spacings.sm};
`);

const _validateIndexPrefix = (value: string) => {
  let error;

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

const IndexSetConfigurationForm = ({
  indexSet,
  rotationStrategies,
  retentionStrategies,
  retentionStrategiesContext,
  create,
  onUpdate,
  history,
  cancelLink,
  submitButtonText,
  submitLoadingText,
} : Props) => {
  const retentionConfigSegments = [
    { value: 'data_tiering', label: 'Data Tiering' },
    { value: 'legacy', label: 'Legacy (Deprecated)' },
  ];
  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');
  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<string>('data_tiering');

  const saveConfiguration = (values: IndexSet) => onUpdate(prepareDataTieringConfig(values));

  const onFieldTypeRefreshIntervalChange = (
    intervalValue: number,
    unit: Unit,
    name: string,
    onChange: (name: string, value: number) => void,
    setFieldValue: (key: string, value: number) => void) => {
    onChange(name, moment.duration(intervalValue, unit).asMilliseconds());
    setFieldValue(name, moment.duration(intervalValue, unit).asMilliseconds());
    setFieldTypeRefreshIntervalUnit(unit);
  };

  const {
    rotation_strategy: indexSetRotationStrategy,
    rotation_strategy_class: indexSetRotationStrategyClass,
    retention_strategy: indexSetRetentionStrategy,
    retention_strategy_class: IndexSetRetentionStrategyClass,
  } = indexSet;

  const rotationConfig = () : React.ReactNode => {
    if (!rotationStrategies) {
      return <Spinner />;
    }

    const activeConfig = {
      config: indexSetRotationStrategy,
      strategy: indexSetRotationStrategyClass,
    };

    return (
      <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                               name="rotation"
                                               description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                               selectPlaceholder="Select rotation strategy"
                                               pluginExports={PluginStore.exports('indexRotationConfig')}
                                               strategies={rotationStrategies}
                                               activeConfig={activeConfig}
                                               getState={_getRotationConfigState} />
    );
  };

  const retentionConfig = (): React.ReactNode => {
    if (!retentionStrategies) {
      return <Spinner />;
    }

    const activeConfig = {
      config: indexSetRetentionStrategy,
      strategy: IndexSetRetentionStrategyClass,
    };

    return (
      <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                               name="retention"
                                               description="Graylog uses a retention strategy to clean up old indices."
                                               selectPlaceholder="Select retention strategy"
                                               pluginExports={PluginStore.exports('indexRetentionConfig')}
                                               strategies={retentionStrategies}
                                               retentionStrategiesContext={retentionStrategiesContext}
                                               activeConfig={activeConfig}
                                               getState={_getRetentionConfigState} />
    );
  };

  const onCancel = () => history.push(cancelLink);

  const isCloud = AppConfig.isCloud();

  return (
    <Row>
      <Col md={8}>
        <Formik onSubmit={saveConfiguration}
                initialValues={indexSet}>
          {({ isValid, setFieldValue, isSubmitting, values }) => (
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
                    {create && (
                    <span>
                      <FormikFormGroup type="text"
                                       label="Index prefix"
                                       name="index_prefix"
                                       help={(
                                         <span>
                                           A <strong>unique</strong> prefix used in Elasticsearch indices belonging to this index set.
                                           The prefix must start with a letter or number, and can only contain letters, numbers, &apos;_&apos;, &apos;-&apos; and &apos;+&apos;.
                                         </span>
                                       )}
                                       validate={_validateIndexPrefix}
                                       required />
                      <FormikFormGroup type="text"
                                       label="Analyzer"
                                       name="index_analyzer"
                                       help="Elasticsearch analyzer for this index set."
                                       required />
                    </span>
                    )}
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
                        {({ field: { name, value, onChange } }) => (
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
                                           update={(intervalValue: number, unit: Unit) => onFieldTypeRefreshIntervalChange(
                                             intervalValue, unit, name, onChange, setFieldValue,
                                           )} />
                          </Input>

                        )}
                      </Field>
                    </HideOnCloud>
                  </Col>
                </Row>
                {isCloud ? (
                  <>
                    {indexSet.writable && rotationConfig()}
                    {indexSet.writable && retentionConfig()}
                  </>
                ) : (
                  <>
                    <ConfigSegmentsTitle>Rotation and Retention</ConfigSegmentsTitle>
                    <SegmentedControl data={retentionConfigSegments}
                                      value={selectedRetentionSegment}
                                      handleChange={setSelectedRetentionSegment} />

                    {selectedRetentionSegment === 'data_tiering' ? (
                      <ConfigSegment>
                        <h3>Data Tiering Configuration</h3>
                        <DataTieringVisualisation minDays={values.data_tiering?.index_lifetime_min}
                                                  maxDays={values.data_tiering?.index_lifetime_max}
                                                  minDaysInHot={values.data_tiering?.index_hot_lifetime_min} />
                        <DataTieringConfiguration />
                      </ConfigSegment>
                    )
                      : (
                        <ConfigSegment>
                          {indexSet.writable && rotationConfig()}
                          {indexSet.writable && retentionConfig()}
                        </ConfigSegment>
                      )}

                  </>
                )}

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

// @ts-ignore
export default withHistory(IndexSetConfigurationForm);

IndexSetConfigurationForm.defaultProps = {
  create: false,
  submitLoadingText: 'Updating Index Set...',
};
