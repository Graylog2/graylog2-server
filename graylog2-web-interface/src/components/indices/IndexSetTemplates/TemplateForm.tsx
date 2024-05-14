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

import styled, { css } from 'styled-components';
import React, { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
} from 'components/indices/Types';
import type { IndexSetTemplateFormValues, IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { Col, SegmentedControl } from 'components/bootstrap';
import { FormikInput, FormSubmit, InputOptionalInfo, Spinner, TimeUnitInput } from 'components/common';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import { prepareDataTieringConfig, prepareDataTieringInitialValues, DataTieringConfiguration } from 'components/indices/data-tiering';

type Props = {
  initialValues?: IndexSetTemplate,
  submitButtonText: string,
  submitLoadingText: string,
  onCancel: () => void,
  onSubmit: (template: IndexSetTemplate) => void
}

type RetentionConfigSegment = 'data_tiering' | 'legacy';

type RotationStrategiesProps = {
  rotationStrategies: Array<any>,
  indexSetRotationStrategy?: RotationStrategyConfig,
  indexSetRotationStrategyClass?: string
}

type RetentionConfigProps = {
  retentionStrategies: Array<any>,
  retentionStrategiesContext: RetentionStrategyContext,
  indexSetRetentionStrategy?: RetentionStrategyConfig,
  indexSetRetentionStrategyClass?: string
}

const TIME_UNITS = ['SECONDS', 'MINUTES'];

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 30px;
`;

const ConfigSegmentsTitle = styled.h2(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const ConfigSegment = styled.div(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
  margin-top: ${theme.spacings.md};
`);

const getRotationConfigState = (strategy: string, data: RotationStrategyConfig) => ({
  rotation_strategy_class: strategy,
  rotation_strategy: data,
});

const getRetentionConfigState = (strategy: string, data: RetentionStrategyConfig) => ({
  retention_strategy_class: strategy,
  retention_strategy: data,
});

const RotationConfig = ({ rotationStrategies, indexSetRotationStrategy, indexSetRotationStrategyClass }: RotationStrategiesProps) => {
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
                                             getState={getRotationConfigState} />
  );
};

RotationConfig.defaultProps = {
  indexSetRotationStrategy: undefined,
  indexSetRotationStrategyClass: undefined,
};

const RetentionConfig = ({ retentionStrategies, retentionStrategiesContext, indexSetRetentionStrategy, indexSetRetentionStrategyClass }: RetentionConfigProps) => {
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
                                               strategy: indexSetRetentionStrategyClass,
                                             }}
                                             getState={getRetentionConfigState} />
  );
};

RetentionConfig.defaultProps = {
  indexSetRetentionStrategy: undefined,
  indexSetRetentionStrategyClass: undefined,
};

const validate = (formValues: IndexSetTemplateFormValues) => {
  let errors: {
    title?: string,
    index_set_config?: {
      index_analyzer?: string,
      shards?: string,
      replicas?: string,
      index_optimization_max_num_segments?: string,
    }
  } = { };

  if (!formValues.title) {
    errors.title = 'Template title is required';
  }

  if (!formValues.index_set_config.index_analyzer) {
    errors = { ...errors, index_set_config: { ...errors.index_set_config, index_analyzer: 'Index analyzer is required' } };
  }

  if (!formValues.index_set_config.shards) {
    errors = { ...errors, index_set_config: { ...errors.index_set_config, shards: 'Shards is required' } };
  }

  if (!formValues.index_set_config.replicas) {
    errors = { ...errors, index_set_config: { ...errors.index_set_config, replicas: 'Replicas is required' } };
  }

  if (!formValues.index_set_config.index_optimization_max_num_segments) {
    errors = { ...errors, index_set_config: { ...errors.index_set_config, index_optimization_max_num_segments: 'Max. number of segments is required' } };
  }

  return errors;
};

const TemplateForm = ({ initialValues, submitButtonText, submitLoadingText, onCancel, onSubmit }: Props) => {
  const retentionConfigSegments: Array<{value: RetentionConfigSegment, label: string}> = [
    { value: 'data_tiering', label: 'Data Tiering' },
    { value: 'legacy', label: 'Legacy (Deprecated)' },
  ];

  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<RetentionConfigSegment>('data_tiering');

  useEffect(() => {
    if (selectedRetentionSegment === 'legacy') {
      IndicesConfigurationActions.loadRotationStrategies();
      IndicesConfigurationActions.loadRetentionStrategies();
    }
  }, [selectedRetentionSegment]);

  const { rotationStrategies, retentionStrategies, retentionStrategiesContext } = useStore(IndicesConfigurationStore, (state) => state);

  const handleSubmit = (values: IndexSetTemplateFormValues) => {
    let template = {};
    const { retention_strategy, retention_strategy_class, rotation_strategy, rotation_strategy_class, ...other } = values;

    const index_set_config = { ...values.index_set_config };

    template = { ...other };

    template = {
      ...template,
      index_set_config: {
        ...index_set_config,
        retention_strategy: selectedRetentionSegment === 'legacy' ? retention_strategy : undefined,
        retention_strategy_class: selectedRetentionSegment === 'legacy' ? retention_strategy_class : undefined,
        rotation_strategy: selectedRetentionSegment === 'legacy' ? rotation_strategy : undefined,
        rotation_strategy_class: selectedRetentionSegment === 'legacy' ? rotation_strategy_class : undefined,
        use_legacy_rotation: selectedRetentionSegment === 'legacy',
        data_tiering: selectedRetentionSegment === 'legacy' ? undefined : prepareDataTieringConfig(index_set_config.data_tiering, PluginStore),
      },
    };

    onSubmit(template as unknown as IndexSetTemplate);
  };

  const prepareInitialValues = () => {
    const values = { index_set_config: {}, ...initialValues };

    const { rotation_strategy, rotation_strategy_class, retention_strategy, retention_strategy_class } = values.index_set_config;

    delete values.index_set_config.rotation_strategy;
    delete values.index_set_config.rotation_strategy_class;
    delete values.index_set_config.retention_strategy;
    delete values.index_set_config.retention_strategy_class;

    const data_tiering = prepareDataTieringInitialValues(values.index_set_config.data_tiering, PluginStore);

    return {
      ...values,
      rotation_strategy,
      rotation_strategy_class,
      retention_strategy,
      retention_strategy_class,
      index_set_config: {
        index_analyzer: 'standard',
        shards: 4,
        replicas: 1,
        index_optimization_max_num_segments: 1,
        index_optimization_disabled: false,
        field_type_refresh_interval: 1,
        field_type_refresh_interval_unit: 'SECONDS',
        use_legacy_rotation: false,
        ...values.index_set_config,
        data_tiering,
      },
    } as unknown as IndexSetTemplateFormValues;
  };

  return (
    <Col lg={8}>
      <Formik<IndexSetTemplateFormValues> initialValues={prepareInitialValues()}
                                          onSubmit={handleSubmit}
                                          validate={validate}
                                          validateOnChange>
        {({ isSubmitting, isValid, isValidating, setFieldValue, values }) => (
          <Form>
            <FormikInput name="title"
                         label="Template title"
                         id="index-set-template-title"
                         help="A descriptive title of the template"
                         required />
            <FormikInput name="description"
                         id="index-set-template-description"
                         label={<>Description <InputOptionalInfo /></>}
                         type="textarea"
                         help="Longer description for template"
                         rows={6} />
            <FormikInput name="index_set_config.index_analyzer"
                         label="Index Analyzer"
                         id="index-set-template-index-analyzer"
                         help="Index Analyzer" />
            <FormikInput name="index_set_config.shards"
                         label="Shards"
                         type="number"
                         id="index-set-template-shards"
                         help="Number of shards used per index in this index set" />
            <FormikInput name="index_set_config.replicas"
                         label="Replicas"
                         type="number"
                         id="index-set-template-replicas"
                         help="Number of replicas used per index in this index set" />
            <FormikInput name="index_set_config.index_optimization_max_num_segments"
                         label="Max. number of segments"
                         type="number"
                         id="index-set-template-index-optimization-max-num-segments"
                         help="Maximum number of segments per index after optimization (force merge)" />
            <FormikInput type="checkbox"
                         name="index_set_config.index_optimization_disabled"
                         id="index-set-template-index-optimization-disabled"
                         label="Disable index optimization after rotation"
                         help="Disable Elasticsearch index optimization (force merge) after rotation" />
            <TimeUnitInput label="Field type refresh interval"
                           update={(value, unit) => {
                             setFieldValue('index_set_config.field_type_refresh_interval', value);
                             setFieldValue('index_set_config.field_type_refresh_interval_unit', unit);
                           }}
                           value={values.index_set_config.field_type_refresh_interval}
                           unit={values.index_set_config.field_type_refresh_interval_unit}
                           enabled
                           hideCheckbox
                           units={TIME_UNITS} />

            <>
              <ConfigSegmentsTitle>Rotation and Retention</ConfigSegmentsTitle>
              <SegmentedControl<RetentionConfigSegment> data={retentionConfigSegments}
                                                        value={selectedRetentionSegment}
                                                        onChange={setSelectedRetentionSegment} />

              {selectedRetentionSegment === 'data_tiering' ? (
                <ConfigSegment>
                  <h3>Data Tiering Configuration</h3>
                  <DataTieringConfiguration valuesPrefix="index_set_config" />
                </ConfigSegment>
              )
                : (
                  <ConfigSegment>
                    <RotationConfig rotationStrategies={rotationStrategies} indexSetRotationStrategy={values.rotation_strategy} indexSetRotationStrategyClass={values.rotation_strategy_class} />
                    <RetentionConfig retentionStrategies={retentionStrategies} retentionStrategiesContext={retentionStrategiesContext} indexSetRetentionStrategy={values.retention_strategy} indexSetRetentionStrategyClass={values.retention_strategy_class} />
                  </ConfigSegment>
                )}

            </>

            <StyledFormSubmit submitButtonText={submitButtonText}
                              onCancel={onCancel}
                              disabledSubmit={isValidating || !isValid}
                              isSubmitting={isSubmitting}
                              submitLoadingText={submitLoadingText} />
          </Form>
        )}
      </Formik>
    </Col>
  );
};

export default TemplateForm;
