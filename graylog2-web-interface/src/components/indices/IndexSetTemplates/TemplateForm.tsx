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
import React, { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Formik, Form, Field } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';
import cloneDeep from 'lodash/cloneDeep';
import type { TFunction } from 'i18next';

import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
} from 'components/indices/Types';
import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import type { IndexSetTemplateFormValues, IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { Col, Row, SegmentedControl } from 'components/bootstrap';
import { FormikInput, FormSubmit, InputOptionalInfo, Section, Spinner, TimeUnitInput } from 'components/common';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';
import {
  prepareDataTieringConfig,
  prepareDataTieringInitialValues,
  DataTieringConfiguration,
} from 'components/indices/data-tiering';
import useProductName from 'brand-customization/useProductName';
import useIndicesConfiguration from 'hooks/useIndicesConfiguration';

type Props = {
  initialValues?: IndexSetTemplate;
  submitButtonText: string;
  submitLoadingText: string;
  onCancel: () => void;
  onSubmit: (template: IndexSetTemplate) => void;
};

type RetentionConfigSegment = 'data_tiering' | 'legacy';

type RotationStrategiesProps = {
  rotationStrategies: Array<any>;
  indexSetRotationStrategy?: RotationStrategyConfig;
  indexSetRotationStrategyClass?: string;
};

type RetentionConfigProps = {
  retentionStrategies: Array<any>;
  retentionStrategiesContext: RetentionStrategyContext;
  indexSetRetentionStrategy?: RetentionStrategyConfig;
  indexSetRetentionStrategyClass?: string;
};

type Unit = 'seconds' | 'minutes';

const TIME_UNITS = ['SECONDS', 'MINUTES'];

const ConfigSegment = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
  `,
);

const FlexWrapper = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
  `,
);

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

const RotationConfig = ({
  rotationStrategies,
  indexSetRotationStrategy = undefined,
  indexSetRotationStrategyClass = undefined,
}: RotationStrategiesProps) => {
  const { t } = useTranslation('indexsets');

  if (!rotationStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration
      title={t('index_set_form.rotation_title')}
      name="rotation"
      label={t('index_set_form.rotation_label')}
      description={t('templates.rotation_description')}
      selectPlaceholder={t('index_set_form.rotation_placeholder')}
      pluginExports={PluginStore.exports('indexRotationConfig')}
      strategies={rotationStrategies}
      activeConfig={{
        config: indexSetRotationStrategy,
        strategy: indexSetRotationStrategyClass,
      }}
      getState={getRotationConfigState}
    />
  );
};

const RetentionConfig = ({
  retentionStrategies,
  retentionStrategiesContext,
  indexSetRetentionStrategy = undefined,
  indexSetRetentionStrategyClass = undefined,
}: RetentionConfigProps) => {
  const { t } = useTranslation('indexsets');

  if (!retentionStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration
      title={t('index_set_form.retention_title')}
      name="retention"
      label={t('index_set_form.retention_label')}
      description={t('index_set_form.retention_description')}
      selectPlaceholder={t('index_set_form.retention_placeholder')}
      pluginExports={PluginStore.exports('indexRetentionConfig')}
      strategies={retentionStrategies}
      retentionStrategiesContext={retentionStrategiesContext}
      activeConfig={{
        config: indexSetRetentionStrategy,
        strategy: indexSetRetentionStrategyClass,
      }}
      getState={getRetentionConfigState}
    />
  );
};

const validate = (formValues: IndexSetTemplateFormValues, usesLegacyRetention: boolean, t: TFunction) => {
  let errors: {
    title?: string;
    retention_strategy_class?: string;
    rotation_strategy_class?: string;
    index_set_config?: {
      index_analyzer?: string;
      shards?: string;
      replicas?: string;
      index_optimization_max_num_segments?: string;
    };
  } = {};

  if (!formValues.title) {
    errors.title = t('templates.error_title_required');
  }

  if (!formValues.index_set_config.index_analyzer) {
    errors = {
      ...errors,
      index_set_config: { ...errors.index_set_config, index_analyzer: t('templates.error_analyzer_required') },
    };
  }

  if (!formValues.index_set_config.shards) {
    errors = {
      ...errors,
      index_set_config: { ...errors.index_set_config, shards: t('templates.error_shards_required') },
    };
  }

  if (!formValues.index_set_config.index_optimization_max_num_segments) {
    errors = {
      ...errors,
      index_set_config: {
        ...errors.index_set_config,
        index_optimization_max_num_segments: t('templates.error_segments_required'),
      },
    };
  }

  if (usesLegacyRetention) {
    if (!formValues.retention_strategy_class) {
      errors = { ...errors, retention_strategy_class: t('templates.error_retention_required') };
    }

    if (!formValues.rotation_strategy_class) {
      errors = { ...errors, rotation_strategy_class: t('templates.error_rotation_required') };
    }
  }

  return errors;
};

const TemplateForm = ({
  initialValues = undefined,
  submitButtonText,
  submitLoadingText,
  onCancel,
  onSubmit,
}: Props) => {
  const { t } = useTranslation('indexsets');
  const productName = useProductName();
  const retentionConfigSegments: Array<{ value: RetentionConfigSegment; label: string }> = [
    { value: 'data_tiering', label: t('index_set_form.segment_data_tiering') },
    { value: 'legacy', label: t('index_set_form.segment_legacy') },
  ];

  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();

  const initialSelectedSegment = initialValues?.index_set_config?.use_legacy_rotation ? 'legacy' : 'data_tiering';
  const [selectedRetentionSegment, setSelectedRetentionSegment] =
    useState<RetentionConfigSegment>(initialSelectedSegment);
  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');

  const { rotationStrategies, retentionStrategies, retentionStrategiesContext } = useIndicesConfiguration({
    enabled: selectedRetentionSegment === 'legacy',
  });

  const handleSubmit = (values: IndexSetTemplateFormValues) => {
    let template = {};
    const { retention_strategy, retention_strategy_class, rotation_strategy, rotation_strategy_class, ...other } =
      values;

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
        data_tiering:
          selectedRetentionSegment === 'legacy'
            ? undefined
            : prepareDataTieringConfig(index_set_config.data_tiering, PluginStore, false),
      },
    };

    onSubmit(template as unknown as IndexSetTemplate);
  };

  const prepareInitialValues = () => {
    const existingTemplate = cloneDeep(initialValues);
    const values = { index_set_config: indexSetTemplateDefaults, ...existingTemplate };

    const {
      rotation_strategy,
      rotation_strategy_class,
      retention_strategy,
      retention_strategy_class,
      ...indexSetConfig
    } = values.index_set_config;

    const data_tiering = prepareDataTieringInitialValues(values.index_set_config.data_tiering, PluginStore, false);

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
    setFieldValue: (key: string, value: number) => void,
  ) => {
    onChange(name, moment.duration(intervalValue, unit).asMilliseconds());
    setFieldValue(name, moment.duration(intervalValue, unit).asMilliseconds());
    setFieldTypeRefreshIntervalUnit(unit);
  };

  if (loadingIndexSetTemplateDefaults) return <Spinner />;

  if (!indexSetTemplateDefaults) return null;

  return (
    <Row>
      <Col md={12}>
        <Formik<IndexSetTemplateFormValues>
          initialValues={prepareInitialValues()}
          onSubmit={handleSubmit}
          enableReinitialize
          validate={(values) => validate(values, selectedRetentionSegment === 'legacy', t)}
          validateOnChange>
          {({ isSubmitting, isValid, isValidating, setFieldValue, values }) => (
            <IndexRetentionProvider>
              <Form>
                <FlexWrapper>
                  <Section title={t('index_set_form.configuration_information')}>
                    <FormikInput
                      name="title"
                      label={t('templates.form_title_label')}
                      id="index-set-template-title"
                      help={t('templates.form_title_help')}
                      required
                    />
                    <FormikInput
                      name="description"
                      id="index-set-template-description"
                      label={
                        <>
                          {t('templates.description')} <InputOptionalInfo />
                        </>
                      }
                      type="textarea"
                      help={t('templates.form_description_help')}
                      rows={6}
                    />
                  </Section>
                  <Section title={t('index_set_form.details')}>
                    <FormikInput
                      name="index_set_config.index_analyzer"
                      label={t('templates.form_analyzer_label')}
                      id="index-set-template-index-analyzer"
                      help={t('templates.form_analyzer_label')}
                    />
                    <FormikInput
                      name="index_set_config.shards"
                      label={t('index_set_form.shards_label')}
                      type="number"
                      id="index-set-template-shards"
                      help={t('index_set_form.shards_help')}
                    />
                    <FormikInput
                      name="index_set_config.replicas"
                      label={t('index_set_form.replicas_label')}
                      type="number"
                      id="index-set-template-replicas"
                      help={t('index_set_form.replicas_help')}
                    />
                    <FormikInput
                      name="index_set_config.index_optimization_max_num_segments"
                      label={t('index_set_form.max_segments_label')}
                      type="number"
                      id="index-set-template-index-optimization-max-num-segments"
                      help={<Trans t={t} i18nKey="index_set_form.max_segments_help" components={{ em: <em /> }} />}
                    />
                    <FormikInput
                      type="checkbox"
                      name="index_set_config.index_optimization_disabled"
                      id="index-set-template-index-optimization-disabled"
                      label={t('index_set_form.disable_optimization_label')}
                      help={
                        <Trans t={t} i18nKey="index_set_form.disable_optimization_help" components={{ em: <em /> }} />
                      }
                    />
                    <Field name="index_set_config.field_type_refresh_interval">
                      {({ field: { name, value, onChange } }) => (
                        <TimeUnitInput
                          label={t('index_set_form.field_type_refresh_label')}
                          help={
                            <Trans
                              t={t}
                              i18nKey="index_set_form.field_type_refresh_help"
                              values={{ productName }}
                              components={{ em: <em /> }}
                            />
                          }
                          value={moment.duration(value, 'milliseconds').as(fieldTypeRefreshIntervalUnit)}
                          unit={fieldTypeRefreshIntervalUnit.toUpperCase()}
                          units={TIME_UNITS}
                          required
                          update={(intervalValue: number, unit: Unit) =>
                            onFieldTypeRefreshIntervalChange(intervalValue, unit, name, onChange, setFieldValue)
                          }
                        />
                      )}
                    </Field>
                  </Section>

                  <Section title={t('index_set_form.rotation_retention')}>
                    <SegmentedControl<RetentionConfigSegment>
                      data={retentionConfigSegments}
                      value={selectedRetentionSegment}
                      onChange={setSelectedRetentionSegment}
                    />
                    {selectedRetentionSegment === 'data_tiering' ? (
                      <ConfigSegment>
                        <DataTieringConfiguration valuesPrefix="index_set_config" ignoreFieldRestrictions />
                      </ConfigSegment>
                    ) : (
                      <ConfigSegment>
                        <RotationConfig
                          rotationStrategies={rotationStrategies}
                          indexSetRotationStrategy={values.rotation_strategy}
                          indexSetRotationStrategyClass={values.rotation_strategy_class}
                        />
                        <RetentionConfig
                          retentionStrategies={retentionStrategies}
                          retentionStrategiesContext={retentionStrategiesContext}
                          indexSetRetentionStrategy={values.retention_strategy}
                          indexSetRetentionStrategyClass={values.retention_strategy_class}
                        />
                      </ConfigSegment>
                    )}
                  </Section>
                  <SubmitWrapper>
                    <FormSubmit
                      submitButtonText={submitButtonText}
                      onCancel={onCancel}
                      disabledSubmit={isValidating || !isValid}
                      isSubmitting={isSubmitting}
                      submitLoadingText={submitLoadingText}
                    />
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
