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
import moment from 'moment';
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Formik, Form, Field } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';
import cloneDeep from 'lodash/cloneDeep';

import { useStore } from 'stores/connect';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
} from 'components/indices/Types';
import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import type { IndexSetTemplateFormValues, IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { indexSetTemplatePropType } from 'components/indices/IndexSetTemplates/types';
import { Col, Row, SegmentedControl } from 'components/bootstrap';
import { FormikInput, FormSubmit, InputOptionalInfo, Section, Spinner, TimeUnitInput } from 'components/common';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';
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

type Unit = 'seconds' | 'minutes';

const TIME_UNITS = ['SECONDS', 'MINUTES'];

const ConfigSegment = styled.div(({ theme }) => css`
  margin-top: ${theme.spacings.md};
`);

const FlexWrapper = styled.div(({ theme }) => css`
  display: flex;
  flex-direction: column;
  gap: ${theme.spacings.md};
`);

const SubmitWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

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
                                             label="Rotation strategy"
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
                                             label="Retention strategy"
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

const validate = (formValues: IndexSetTemplateFormValues, usesLegacyRetention: boolean) => {
  let errors: {
    title?: string,
    retention_strategy_class?: string,
    rotation_strategy_class?: string,
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

  if (!formValues.index_set_config.index_optimization_max_num_segments) {
    errors = { ...errors, index_set_config: { ...errors.index_set_config, index_optimization_max_num_segments: 'Max. number of segments is required' } };
  }

  if (usesLegacyRetention) {
    if (!formValues.retention_strategy_class) {
      errors = { ...errors, retention_strategy_class: 'Please select a retention strategy' };
    }

    if (!formValues.rotation_strategy_class) {
      errors = { ...errors, rotation_strategy_class: 'Please select a rotation strategy' };
    }
  }

  return errors;
};

const TemplateForm = ({ initialValues, submitButtonText, submitLoadingText, onCancel, onSubmit }: Props) => {
  const retentionConfigSegments: Array<{value: RetentionConfigSegment, label: string}> = [
    { value: 'data_tiering', label: 'Data Tiering' },
    { value: 'legacy', label: 'Legacy (Deprecated)' },
  ];

  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();

  const initialSelectedSegment = initialValues?.index_set_config?.use_legacy_rotation ? 'legacy' : 'data_tiering';
  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<RetentionConfigSegment>(initialSelectedSegment);
  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');

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
    const existingTemplate = cloneDeep(initialValues);
    const values = { index_set_config: indexSetTemplateDefaults, ...existingTemplate };

    const { rotation_strategy, rotation_strategy_class, retention_strategy, retention_strategy_class, ...indexSetConfig } = values.index_set_config;

    const data_tiering = prepareDataTieringInitialValues(values.index_set_config.data_tiering, PluginStore);

    return {
      ...values,
      rotation_strategy,
      rotation_strategy_class,
      retention_strategy,
      retention_strategy_class,
      index_set_config: {
        ...indexSetConfig,
        data_tiering,
      },
    } as unknown as IndexSetTemplateFormValues;
  };

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

  if (loadingIndexSetTemplateDefaults) return (<Spinner />);

  if (!indexSetTemplateDefaults) return null;

  return (
    <Row>
      <Col md={12}>
        <Formik<IndexSetTemplateFormValues> initialValues={prepareInitialValues()}
                                            onSubmit={handleSubmit}
                                            enableReinitialize
                                            validate={(values) => validate(values, selectedRetentionSegment === 'legacy')}
                                            validateOnChange>
          {({ isSubmitting, isValid, isValidating, setFieldValue, values }) => (
            <IndexRetentionProvider>
              <Form>
                <FlexWrapper>
                  <Section title="Configuration Information">

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
                  </Section>
                  <Section title="Details">

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
                    <Field name="index_set_config.field_type_refresh_interval">
                      {({ field: { name, value, onChange } }) => (
                        <TimeUnitInput id="field-type-refresh-interval"
                                       label="Field type refresh interval"
                                       type="number"
                                       help="How often the field type information for the active write index will be updated."
                                       value={moment.duration(value, 'milliseconds').as(fieldTypeRefreshIntervalUnit)}
                                       unit={fieldTypeRefreshIntervalUnit.toUpperCase()}
                                       units={TIME_UNITS}
                                       required
                                       update={(intervalValue: number, unit: Unit) => onFieldTypeRefreshIntervalChange(
                                         intervalValue, unit, name, onChange, setFieldValue,
                                       )} />
                      )}
                    </Field>
                  </Section>

                  <Section title="Rotation & Retention">
                    <SegmentedControl<RetentionConfigSegment> data={retentionConfigSegments}
                                                              value={selectedRetentionSegment}
                                                              onChange={setSelectedRetentionSegment} />
                    {selectedRetentionSegment === 'data_tiering' ? (
                      <ConfigSegment>
                        <DataTieringConfiguration valuesPrefix="index_set_config" />
                      </ConfigSegment>
                    )
                      : (
                        <ConfigSegment>
                          <RotationConfig rotationStrategies={rotationStrategies}
                                          indexSetRotationStrategy={values.rotation_strategy}
                                          indexSetRotationStrategyClass={values.rotation_strategy_class} />
                          <RetentionConfig retentionStrategies={retentionStrategies}
                                           retentionStrategiesContext={retentionStrategiesContext}
                                           indexSetRetentionStrategy={values.retention_strategy}
                                           indexSetRetentionStrategyClass={values.retention_strategy_class} />
                        </ConfigSegment>
                      )}

                  </Section>
                  <SubmitWrapper>
                    <FormSubmit submitButtonText={submitButtonText}
                                onCancel={onCancel}
                                disabledSubmit={isValidating || !isValid}
                                isSubmitting={isSubmitting}
                                submitLoadingText={submitLoadingText} />
                  </SubmitWrapper>
                </FlexWrapper>
              </Form>
            </IndexRetentionProvider>
          )}
        </Formik>
      </Col>
    </Row>
  );
};

export default TemplateForm;

TemplateForm.propTypes = {
  initialValues: indexSetTemplatePropType,
  submitButtonText: PropTypes.string.isRequired,
  submitLoadingText: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

TemplateForm.defaultProps = {
  initialValues: undefined,
};
