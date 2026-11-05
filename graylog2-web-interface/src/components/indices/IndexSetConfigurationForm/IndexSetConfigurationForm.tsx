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
import React, { useMemo, useState, useCallback } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import moment from 'moment';
import { Formik, Form, Field } from 'formik';
import type { FormikHelpers } from 'formik';
import styled, { css } from 'styled-components';
import deburr from 'lodash/deburr';
import { PluginStore } from 'graylog-web-plugin/plugin';

import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import AppConfig from 'util/AppConfig';
import { ConfirmDialog, FormikInput, FormSubmit, Section, Spinner, TimeUnitInput } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { Alert, Col, Row } from 'components/bootstrap';
import 'components/indices/rotation';
import 'components/indices/retention';
import { prepareDataTieringConfig, prepareDataTieringInitialValues } from 'components/indices/data-tiering';
import type { IndexSet, IndexSetFormValues } from 'stores/indices/IndexSetsStore';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
  RetentionStrategyContext,
  Strategies,
} from 'components/indices/Types';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';
import useHistory from 'routing/useHistory';
import IndexSetProfileConfiguration from 'components/indices/IndexSetProfileConfiguration';
import useFeature from 'hooks/useFeature';
import useIndexSet from 'components/indices/hooks/useIndexSet';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';
import useProductName from 'brand-customization/useProductName';
import HiddenFieldWrapper from 'components/indices/IndexSetConfigurationForm/HiddenFieldWrapper';
import IndexSetReadOnlyConfiguration from 'components/indices/IndexSetConfigurationForm/IndexSetReadOnlyConfiguration';
import IndexSetRotationRetentionConfigurationSection from 'components/indices/IndexSetConfigurationForm/IndexSetRotationRetentionConfigurationSection';
import usePermissions from 'hooks/usePermissions';
import { parseFieldRestrictions } from 'components/indices/helpers/fieldRestrictions';

type Props = {
  cancelLink: string;
  create?: boolean;
  indexSet?: IndexSet;
  onUpdate: (indexSet: IndexSet) => Promise<void> | void;
  retentionStrategies: Strategies;
  retentionStrategiesContext: RetentionStrategyContext;
  rotationStrategies: Strategies;
  submitButtonText: string;
  submitLoadingText?: string;
};

type Unit = 'seconds' | 'minutes';

type RetentionConfigSegment = 'data_tiering' | 'legacy';

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

export const transformTitleToPrefix = (title: string): string => {
  if (!title) return '';

  return deburr(title) // Replace extended letters with basic Latin letters
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_+-]+/g, '-') // Replace invalid chars with hyphen
    .replace(/^[_+-]+/, '') // Remove leading invalid chars
    .replace(/-+$/, '') // Remove trailing hyphens
    .replace(/-{2,}/g, '-'); // Collapse multiple hyphens
};

const IndexSetConfigurationForm = ({
  indexSet: initialIndexSet = undefined,
  rotationStrategies,
  retentionStrategies,
  retentionStrategiesContext,
  create = false,
  onUpdate,
  cancelLink,
  submitButtonText,
  submitLoadingText = undefined,
}: Props) => {
  const { t } = useTranslation('indexsets');
  const history = useHistory();
  const productName = useProductName();
  const { isPermitted } = usePermissions();
  const ignoreFieldRestrictions = isPermitted('indexsets_field_restrictions:edit');

  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');
  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();
  const [indexSet] = useIndexSet(initialIndexSet);
  const [autoFillPrefix, setAutoFillPrefix] = useState<boolean>(true);

  const getFieldRestrictions = useCallback(() => parseFieldRestrictions(indexSet?.field_restrictions), [indexSet]);

  const immutableFields = getFieldRestrictions().immutableFields;
  const hiddenFields = getFieldRestrictions().hiddenFields;

  const isCloud = AppConfig.isCloud();
  const enableDataTieringCloud = useFeature('data_tiering_cloud');

  const initialSegment = (): RetentionConfigSegment => {
    if (hiddenFields.includes('data_tiering')) return 'legacy';
    if (hiddenFields.includes('legacy')) return 'data_tiering';

    if (indexSet?.use_legacy_rotation) return 'legacy';

    return 'data_tiering';
  };

  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<RetentionConfigSegment>(initialSegment());

  const isDataTieringImmutable = useMemo(() => immutableFields.includes('data_tiering'), [immutableFields]);
  const isDataTieringLocked = isDataTieringImmutable && !ignoreFieldRestrictions;

  const prepareRetentionConfigBeforeSubmit = useCallback(
    (values: IndexSetFormValues): IndexSet => {
      const indexSetValues = { ...values };

      if (!create) {
        delete indexSetValues.index_prefix;
        delete indexSetValues.index_analyzer;
        delete indexSetValues.creation_date;
        delete indexSetValues.can_be_default;
        delete indexSetValues.default;
      }

      const legacyConfig = {
        ...indexSetValues,
        data_tiering: indexSetTemplateDefaults.data_tiering,
        use_legacy_rotation: true,
      };

      if (isCloud && !enableDataTieringCloud) {
        return legacyConfig;
      }

      if (selectedRetentionSegment === 'legacy') {
        return legacyConfig;
      }

      const configWithDataTiering = {
        ...indexSetValues,
        data_tiering: prepareDataTieringConfig(values.data_tiering, PluginStore, isDataTieringLocked),
      };

      if (loadingIndexSetTemplateDefaults || !indexSetTemplateDefaults)
        return { ...configWithDataTiering, use_legacy_rotation: false };

      const legacyindexSetTemplateDefaults = {
        rotation_strategy_class: indexSetTemplateDefaults.rotation_strategy_class,
        rotation_strategy: indexSetTemplateDefaults.rotation_strategy as RotationStrategyConfig,
        retention_strategy_class: indexSetTemplateDefaults.retention_strategy_class,
        retention_strategy: indexSetTemplateDefaults.retention_strategy as RetentionStrategyConfig,
      };

      return { ...configWithDataTiering, ...legacyindexSetTemplateDefaults, use_legacy_rotation: false };
    },
    [
      create,
      loadingIndexSetTemplateDefaults,
      indexSetTemplateDefaults,
      selectedRetentionSegment,
      enableDataTieringCloud,
      isCloud,
      isDataTieringLocked,
    ],
  );

  const [confirmation, setConfirmation] = useState<{ title: string; message: string; values: IndexSet } | null>(null);
  const [savingConfirmed, setSavingConfirmed] = useState(false);

  const dataTieringPlugin = PluginStore.exports('dataTiering').find((plugin) => plugin.type === 'hot_warm');
  const useSubmitConfirmation = dataTieringPlugin?.useIndexSetSubmitConfirmation ?? (() => () => null);
  const submitConfirmation = useSubmitConfirmation({ enabled: !create });

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

  const handleTitleChange = useCallback(
    (event: React.BaseSyntheticEvent, setFieldValue: (field: string, value: any) => void) => {
      if (!create || !autoFillPrefix) return;

      const transformedPrefix = transformTitleToPrefix(event.target.value);
      setFieldValue('index_prefix', transformedPrefix);
    },
    [create, autoFillPrefix],
  );

  const handlePrefixChange = useCallback(() => {
    setAutoFillPrefix(false);
  }, []);

  const detailsSectionRenderable = (): boolean => {
    if (create) return true;

    const detailsFieldsEdit = [
      'shards',
      'replicas',
      'index_optimization_max_num_segments',
      'index_optimization_disabled',
      'field_type_refresh_interval',
    ];

    return detailsFieldsEdit.filter((fieldName: string) => !hiddenFields.includes(fieldName)).length > 0;
  };

  if (!indexSet) return null;

  const onCancel = () => history.push(cancelLink);

  if (loadingIndexSetTemplateDefaults) return <Spinner />;

  const prepareInitialValues = () => {
    if (indexSet.data_tiering) {
      return {
        ...indexSet,
        data_tiering: prepareDataTieringInitialValues(indexSet.data_tiering, PluginStore, isDataTieringLocked),
      };
    }

    return indexSet as unknown as IndexSetFormValues;
  };

  const saveConfiguration = (values: IndexSetFormValues, { setSubmitting }: FormikHelpers<IndexSetFormValues>) => {
    const preparedValues = prepareRetentionConfigBeforeSubmit(values);

    const required = submitConfirmation(prepareInitialValues(), preparedValues);

    if (required) {
      setConfirmation({ ...required, values: preparedValues });
      setSubmitting(false);

      return undefined;
    }

    return onUpdate(preparedValues);
  };

  const cancelConfirmation = () => setConfirmation(null);

  const acceptConfirmation = () => {
    const { values } = confirmation;
    cancelConfirmation();
    setSavingConfirmed(true);

    return Promise.resolve()
      .then(() => onUpdate(values))
      .finally(() => setSavingConfirmed(false));
  };

  return (
    <Row>
      <Col md={12}>
        {confirmation && (
          <ConfirmDialog
            show
            title={confirmation.title}
            btnConfirmText={t('index_set_form.save')}
            onCancel={cancelConfirmation}
            onConfirm={acceptConfirmation}>
            {confirmation.message}
          </ConfirmDialog>
        )}
        <Formik validateOnMount onSubmit={saveConfiguration} enableReinitialize initialValues={prepareInitialValues()}>
          {({ isValid, setFieldValue, isSubmitting, values }) => (
            <IndexRetentionProvider>
              <Form>
                <FlexWrapper>
                  <Section title={t('index_set_form.configuration_information')}>
                    <FormikInput
                      type="text"
                      label={t('index_set_form.title_label')}
                      id="title"
                      name="title"
                      help={t('index_set_form.title_help')}
                      onChange={(event) => handleTitleChange(event, setFieldValue)}
                      required
                    />
                    <FormikInput
                      type="text"
                      id="description"
                      label={t('index_set_form.description_label')}
                      name="description"
                      help={t('index_set_form.description_help')}
                      required
                    />
                  </Section>
                  {detailsSectionRenderable() && (
                    <Section title={t('index_set_form.details')}>
                      {create && (
                        <IndexSetReadOnlyConfiguration
                          hiddenFields={hiddenFields}
                          immutableFields={immutableFields}
                          ignoreFieldRestrictions={ignoreFieldRestrictions}
                          onPrefixChange={handlePrefixChange}
                        />
                      )}
                      <HideOnCloud>
                        <HiddenFieldWrapper
                          hiddenFields={hiddenFields}
                          ignoreFieldRestrictions={ignoreFieldRestrictions}>
                          <FormikInput
                            type="number"
                            id="shards"
                            label={t('index_set_form.shards_label')}
                            name="shards"
                            help={t('index_set_form.shards_help')}
                            required
                            disabled={immutableFields?.includes('shards') && !ignoreFieldRestrictions}
                          />
                          <FormikInput
                            type="number"
                            id="replicas"
                            label={t('index_set_form.replicas_label')}
                            name="replicas"
                            help={t('index_set_form.replicas_help')}
                            required
                            disabled={immutableFields?.includes('replicas') && !ignoreFieldRestrictions}
                          />
                          <FormikInput
                            type="number"
                            id="max-number-segments"
                            label={t('index_set_form.max_segments_label')}
                            name="index_optimization_max_num_segments"
                            minLength={1}
                            help={
                              <Trans t={t} i18nKey="index_set_form.max_segments_help" components={{ em: <em /> }} />
                            }
                            required
                            disabled={
                              immutableFields?.includes('index_optimization_max_num_segments') &&
                              !ignoreFieldRestrictions
                            }
                          />
                          <FormikInput
                            type="checkbox"
                            id="index-optimization-disabled"
                            label={t('index_set_form.disable_optimization_label')}
                            name="index_optimization_disabled"
                            help={
                              <Trans
                                t={t}
                                i18nKey="index_set_form.disable_optimization_help"
                                components={{ em: <em /> }}
                              />
                            }
                            disabled={
                              immutableFields?.includes('index_optimization_disabled') && !ignoreFieldRestrictions
                            }
                          />
                          <Field name="field_type_refresh_interval">
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
                                units={['SECONDS', 'MINUTES']}
                                update={(intervalValue: number, unit: Unit) =>
                                  onFieldTypeRefreshIntervalChange(intervalValue, unit, name, onChange, setFieldValue)
                                }
                                required
                                disabled={
                                  immutableFields?.includes('field_type_refresh_interval') && !ignoreFieldRestrictions
                                }
                              />
                            )}
                          </Field>
                        </HiddenFieldWrapper>
                      </HideOnCloud>
                    </Section>
                  )}
                  <IndexSetRotationRetentionConfigurationSection
                    values={values}
                    indexSet={indexSet}
                    retentionStrategies={retentionStrategies}
                    retentionStrategiesContext={retentionStrategiesContext}
                    rotationStrategies={rotationStrategies}
                    hiddenFields={hiddenFields}
                    immutableFields={immutableFields}
                    ignoreFieldRestrictions={ignoreFieldRestrictions}
                    isCloud={isCloud}
                    enableDataTieringCloud={enableDataTieringCloud}
                    selectedRetentionSegment={selectedRetentionSegment}
                    setSelectedRetentionSegment={setSelectedRetentionSegment}
                  />
                  {isIndexFieldTypeChangeAllowed(indexSet) && (
                    <Section title={t('index_set_form.field_type_profile')}>
                      <Field name="field_type_profile">
                        {({ field: { name, value } }) => (
                          <IndexSetProfileConfiguration
                            value={value}
                            onChange={(profileId) => {
                              setFieldValue(name, profileId);
                            }}
                            name={name}
                          />
                        )}
                      </Field>
                    </Section>
                  )}
                  <Section title={t('index_set_form.important_note')}>
                    <Alert bsStyle="info">{t('index_set_form.important_note_text')}</Alert>
                  </Section>
                  <SubmitWrapper>
                    <FormSubmit
                      disabledSubmit={!isValid}
                      submitButtonText={submitButtonText}
                      submitLoadingText={submitLoadingText}
                      isSubmitting={isSubmitting || savingConfirmed}
                      isAsyncSubmit
                      displayCancel
                      onCancel={onCancel}
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

export default IndexSetConfigurationForm;
