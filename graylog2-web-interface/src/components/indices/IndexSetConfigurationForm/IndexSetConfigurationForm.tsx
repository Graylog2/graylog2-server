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
import React, { useEffect, useState, useCallback } from 'react';
import moment from 'moment';
import { Formik, Form, Field } from 'formik';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import AppConfig from 'util/AppConfig';
import { FormikInput, FormSubmit, Section, Spinner, TimeUnitInput } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { Alert, Col, Row } from 'components/bootstrap';
import 'components/indices/rotation';
import 'components/indices/retention';
import { prepareDataTieringConfig, prepareDataTieringInitialValues } from 'components/indices/data-tiering';
import type { IndexSet, IndexSetFormValues, IndexSetFieldRestriction } from 'stores/indices/IndexSetsStore';
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
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

type Props = {
  cancelLink: string;
  create?: boolean;
  indexSet?: IndexSet;
  onUpdate: (indexSet: IndexSet) => void;
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
  const history = useHistory();
  const productName = useProductName();
  const ignoreFieldRestrictions = isPermitted(useCurrentUser().permissions, 'indexsets_field_restrictions:edit');

  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');
  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();
  const [indexSet] = useIndexSet(initialIndexSet);

  const [immutableFields, setImmutableFields] = useState<string[]>([]);
  const [hiddenFields, setHiddenFields] = useState<string[]>([]);

  const isCloud = AppConfig.isCloud();
  const enableDataTieringCloud = useFeature('data_tiering_cloud');

  const initialSegment = (): RetentionConfigSegment => {
    if (hiddenFields.includes('data_tiering')) return 'legacy';
    if (hiddenFields.includes('legacy')) return 'data_tiering';

    if (indexSet?.use_legacy_rotation) return 'legacy';

    return 'data_tiering';
  };

  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<RetentionConfigSegment>(initialSegment());

  const parseFieldRestrictions = (field_restrictions: IndexSetFieldRestriction[]) => {
    const getHidden = () =>
      Object.keys(field_restrictions).filter(
        (field) => field_restrictions[field].filter((restriction) => restriction.type === 'hidden').length > 0,
      );

    const getImmutable = () =>
      Object.keys(field_restrictions).filter(
        (field) => field_restrictions[field].filter((restriction) => restriction.type === 'immutable').length > 0,
      );

    if (field_restrictions) return [getImmutable(), getHidden()];

    return [[], []];
  };

  useEffect(() => {
    if (indexSet?.use_legacy_rotation) {
      setSelectedRetentionSegment('legacy');
    } else {
      setSelectedRetentionSegment('data_tiering');
    }
  }, [indexSet]);

  useEffect(() => {
    const [tmpImmutable, tmpHidden] = parseFieldRestrictions(indexSet?.field_restrictions);
    setImmutableFields(tmpImmutable);
    setHiddenFields(tmpHidden);
  }, [indexSet]);

  const prepareRetentionConfigBeforeSubmit = useCallback(
    (values: IndexSetFormValues): IndexSet => {
      const indexSetValues = values;

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
        data_tiering: prepareDataTieringConfig(values.data_tiering, PluginStore),
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
    ],
  );

  const saveConfiguration = (values: IndexSetFormValues) => onUpdate(prepareRetentionConfigBeforeSubmit(values));

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
      return { ...indexSet, data_tiering: prepareDataTieringInitialValues(indexSet.data_tiering, PluginStore) };
    }

    return indexSet as unknown as IndexSetFormValues;
  };

  return (
    <Row>
      <Col md={12}>
        <Formik validateOnMount onSubmit={saveConfiguration} enableReinitialize initialValues={prepareInitialValues()}>
          {({ isValid, setFieldValue, isSubmitting, values }) => (
            <IndexRetentionProvider>
              <Form>
                <FlexWrapper>
                  <Section title="Configuration Information">
                    <FormikInput
                      type="text"
                      label="Title"
                      id="title"
                      name="title"
                      help="Descriptive name of the index set."
                      required
                    />
                    <FormikInput
                      type="text"
                      id="description"
                      label="Description"
                      name="description"
                      help="Add a description of this index set."
                      required
                    />
                  </Section>
                  {detailsSectionRenderable() && (
                    <Section title="Details">
                      {create && (
                        <IndexSetReadOnlyConfiguration
                          hiddenFields={hiddenFields}
                          immutableFields={immutableFields}
                          ignoreFieldRestrictions={ignoreFieldRestrictions}
                        />
                      )}
                      <HideOnCloud>
                        <HiddenFieldWrapper
                          hiddenFields={hiddenFields}
                          ignoreFieldRestrictions={ignoreFieldRestrictions}>
                          <FormikInput
                            type="number"
                            id="shards"
                            label="Index Shards"
                            name="shards"
                            help="Number of search cluster Shards used per index in this Index Set. Increasing the Index Shards improves the search cluster write speed of data stored to this Index Set by distributing the active write Index over multiple search nodes. Increasing the Index Shards can degrade search performance and increases the memory footprint of the Index. This value should not be set higher than the number of search nodes."
                            required
                            disabled={immutableFields?.includes('shards') && !ignoreFieldRestrictions}
                          />
                          <FormikInput
                            type="number"
                            id="replicas"
                            label="Index Replica"
                            name="replicas"
                            help="Number of search cluster Replica Shards used per Index in this Index Set. Adding Replica Shards improves search performance during parallel reads of the index, such as occurs on dashboards, and is a component of HA and backup strategy. Each Replica Shard set multiplies the storage requirement and memory footprint of the index. This value should not be set higher than the number of search nodes, and typically not higher than 1.                                   "
                            required
                            disabled={immutableFields?.includes('replicas') && !ignoreFieldRestrictions}
                          />
                          <FormikInput
                            type="number"
                            id="max-number-segments"
                            label="Maximum Number of Segments"
                            name="index_optimization_max_num_segments"
                            minLength={1}
                            help={
                              <>
                                <em>Advanced Option.</em> Maximum number of segments per Search Cluster Index after
                                optimization (force merge). Setting higher values decreases the compression ratio of
                                Index Optimization.
                              </>
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
                            label="Disable Index Optimization after Rotation"
                            name="index_optimization_disabled"
                            help={
                              <>
                                <em>Advanced Option.</em> Index Optimization is a compression process that occurs after
                                an active Index has been rotated and reduces the size of an Index on disk. It manifests
                                as a CPU intensive maintenance task performed by the search cluster after Index
                                rotation. Compressing Indexes improves search performance and decreases the storage
                                footprint of Index Sets.
                              </>
                            }
                            disabled={
                              immutableFields?.includes('index_optimization_disabled') && !ignoreFieldRestrictions
                            }
                          />
                          <Field name="field_type_refresh_interval">
                            {({ field: { name, value, onChange } }) => (
                              <TimeUnitInput
                                label="Field Type Refresh Interval"
                                help={
                                  <>
                                    <em>Advanced Option.</em> How often the Field Type Information for the active write
                                    Index will be updated. Setting this value higher can marginally reduce search
                                    cluster overhead and improve performance, but will result in new data messages
                                    longer to be searchable in {productName}.
                                  </>
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
                    <Section title="Field Type Profile">
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
                  <Section title="Important Note">
                    <Alert bsStyle="info">
                      These changes do not apply to any existing indices. They only apply to newly created indices. To
                      apply this to the current index set immediately, rotate the index.
                    </Alert>
                  </Section>
                  <SubmitWrapper>
                    <FormSubmit
                      disabledSubmit={!isValid}
                      submitButtonText={submitButtonText}
                      submitLoadingText={submitLoadingText}
                      isSubmitting={isSubmitting}
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
