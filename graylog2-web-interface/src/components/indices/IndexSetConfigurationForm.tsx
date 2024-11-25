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
import { Alert, Col, Row, SegmentedControl } from 'components/bootstrap';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import 'components/indices/rotation';
import 'components/indices/retention';
import { DataTieringConfiguration, DataTieringVisualisation, prepareDataTieringConfig, prepareDataTieringInitialValues } from 'components/indices/data-tiering';
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

type Props = {
  cancelLink: string,
  create?: boolean,
  indexSet?: IndexSet,
  onUpdate: (indexSet: IndexSet) => void,
  retentionStrategies: Strategies,
  retentionStrategiesContext: RetentionStrategyContext,
  rotationStrategies: Strategies,
  submitButtonText: string,
  submitLoadingText?: string,
};

type RotationStrategiesProps = {
  rotationStrategies: Array<any>,
  indexSetRotationStrategy: RotationStrategyConfig,
  indexSetRotationStrategyClass: string
}

type RetentionConfigProps = {
  retentionStrategies: Array<any>,
  retentionStrategiesContext: RetentionStrategyContext,
  indexSetRetentionStrategy: RetentionStrategyConfig,
  IndexSetRetentionStrategyClass: string
}

type Unit = 'seconds' | 'minutes';

type RetentionConfigSegment = 'data_tiering' | 'legacy'

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

const RotationStrategies = ({ rotationStrategies, indexSetRotationStrategy, indexSetRotationStrategyClass }: RotationStrategiesProps) => {
  if (!rotationStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                             name="rotation"
                                             description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                             selectPlaceholder="Select rotation strategy"
                                             label="Rotation strategy"
                                             pluginExports={PluginStore.exports('indexRotationConfig')}
                                             strategies={rotationStrategies}
                                             activeConfig={{
                                               config: indexSetRotationStrategy,
                                               strategy: indexSetRotationStrategyClass,
                                             }}
                                             getState={_getRotationConfigState} />
  );
};

const RetentionConfig = ({ retentionStrategies, retentionStrategiesContext, indexSetRetentionStrategy, IndexSetRetentionStrategyClass }: RetentionConfigProps) => {
  if (!retentionStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                             name="retention"
                                             description="Graylog uses a retention strategy to clean up old indices."
                                             selectPlaceholder="Select retention strategy"
                                             label="Retention strategy"
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
      <FormikInput type="text"
                   id="index-prefix"
                   label="Index prefix"
                   name="index_prefix"
                   help={indexPrefixHelp}
                   validate={_validateIndexPrefix}
                   required />
      <FormikInput type="text"
                   id="index-analyzer"
                   label="Analyzer"
                   name="index_analyzer"
                   help="Elasticsearch analyzer for this index set."
                   required />
    </span>
  );
};

const IndexSetConfigurationForm = ({
  indexSet: initialIndexSet,
  rotationStrategies,
  retentionStrategies,
  retentionStrategiesContext,
  create = false,
  onUpdate,
  cancelLink,
  submitButtonText,
  submitLoadingText,
} : Props) => {
  const history = useHistory();

  const [fieldTypeRefreshIntervalUnit, setFieldTypeRefreshIntervalUnit] = useState<Unit>('seconds');
  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();
  const [indexSet] = useIndexSet(initialIndexSet);
  const isCloud = AppConfig.isCloud();
  const enableDataTieringCloud = useFeature('data_tiering_cloud');

  const retentionConfigSegments: Array<{value: RetentionConfigSegment, label: string}> = [
    { value: 'data_tiering', label: 'Data Tiering' },
    { value: 'legacy', label: 'Legacy (Deprecated)' },
  ];

  const initialSegment = () : RetentionConfigSegment => {
    if (indexSet?.use_legacy_rotation) return 'legacy';

    return 'data_tiering';
  };

  const [selectedRetentionSegment, setSelectedRetentionSegment] = useState<RetentionConfigSegment>(initialSegment());

  useEffect(() => {
    if (indexSet?.use_legacy_rotation) {
      setSelectedRetentionSegment('legacy');
    } else {
      setSelectedRetentionSegment('data_tiering');
    }
  }, [indexSet]);

  const prepareRetentionConfigBeforeSubmit = useCallback((values: IndexSetFormValues) : IndexSet => {
    const legacyConfig = { ...values, data_tiering: indexSetTemplateDefaults.data_tiering, use_legacy_rotation: true };

    if (isCloud && !enableDataTieringCloud) {
      return legacyConfig;
    }

    if (selectedRetentionSegment === 'legacy') {
      return legacyConfig;
    }

    const configWithDataTiering = { ...values, data_tiering: prepareDataTieringConfig(values.data_tiering, PluginStore) };

    if (loadingIndexSetTemplateDefaults || !indexSetTemplateDefaults) return { ...configWithDataTiering, use_legacy_rotation: false };

    const legacyindexSetTemplateDefaults = {
      rotation_strategy_class: indexSetTemplateDefaults.rotation_strategy_class,
      rotation_strategy: indexSetTemplateDefaults.rotation_strategy as RotationStrategyConfig,
      retention_strategy_class: indexSetTemplateDefaults.retention_strategy_class,
      retention_strategy: indexSetTemplateDefaults.retention_strategy as RetentionStrategyConfig,
    };

    return { ...configWithDataTiering, ...legacyindexSetTemplateDefaults, use_legacy_rotation: false };
  }, [loadingIndexSetTemplateDefaults, indexSetTemplateDefaults, selectedRetentionSegment, enableDataTieringCloud, isCloud]);

  const saveConfiguration = (values: IndexSetFormValues) => onUpdate(prepareRetentionConfigBeforeSubmit(values));

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

  if (!indexSet) return null;

  const onCancel = () => history.push(cancelLink);

  if (loadingIndexSetTemplateDefaults) return (<Spinner />);

  const prepareInitialValues = () => {
    if (indexSet.data_tiering) {
      return { ...indexSet, data_tiering: prepareDataTieringInitialValues(indexSet.data_tiering, PluginStore) };
    }

    return indexSet as unknown as IndexSetFormValues;
  };

  return (
    <Row>
      <Col md={12}>
        <Formik onSubmit={saveConfiguration}
                enableReinitialize
                initialValues={prepareInitialValues()}>
          {({ isValid, setFieldValue, isSubmitting, values }) => (
            <IndexRetentionProvider>
              <Form>
                <FlexWrapper>
                  <Section title="Configuration Information">
                    <FormikInput type="text"
                                 label="Title"
                                 id="title"
                                 name="title"
                                 help="Descriptive name of the index set."
                                 required />
                    <FormikInput type="text"
                                 id="description"
                                 label="Description"
                                 name="description"
                                 help="Add a description of this index set."
                                 required />
                  </Section>
                  <Section title="Details">
                    {create && <ReadOnlyConfig />}
                    <HideOnCloud>
                      <FormikInput type="number"
                                   id="shards"
                                   label="Index Shards"
                                   name="shards"
                                   help="Number of search cluster Shards used per index in this Index Set. Increasing the Index Shards improves the search cluster write speed of data stored to this Index Set by distributing the active write Index over multiple search nodes. Increasing the Index Shards can degrade search performance and increases the memory footprint of the Index. This value should not be set higher than the number of search nodes."
                                   required />
                      <FormikInput type="number"
                                   id="replicas"
                                   label="Index Replica"
                                   name="replicas"
                                   help="Number of search cluster Replica Shards used per Index in this Index Set. Adding Replica Shards improves search performance during parallel reads of the index, such as occurs on dashboards, and is a component of HA and backup strategy. Each Replica Shard set multiplies the storage requirement and memory footprint of the index. This value should not be set higher than the number of search nodes, and typically not higher than 1.                                   "
                                   required />
                      <FormikInput type="number"
                                   id="max-number-segments"
                                   label="Maximum Number of Segments"
                                   name="index_optimization_max_num_segments"
                                   minLength={1}
                                   help={<><em>Advanced Option.</em> Maximum number of segments per Search Cluster Index after optimization (force merge). Setting higher values decreases the compression ratio of Index Optimization.</>}
                                   required />
                      <FormikInput type="checkbox"
                                   id="index-optimization-disabled"
                                   label="Disable Index Optimization after Rotation"
                                   name="index_optimization_disabled"
                                   help={<><em>Advanced Option.</em> Index Optimization is a compression process that occurs after an active Index has been rotated and reduces the size of an Index on disk. It manifests as a CPU intensive maintenance task performed by the search cluster after Index rotation. Compressing Indexes improves search performance and decreases the storage footprint of Index Sets.</>} />
                      <Field name="field_type_refresh_interval">
                        {({ field: { name, value, onChange } }) => (
                          <TimeUnitInput id="field-type-refresh-interval"
                                         label="Field Type Refresh Interval"
                                         type="number"
                                         help={<><em>Advanced Option.</em> How often the Field Type Information for the active write Index will be updated. Setting this value higher can marginally reduce search cluster overhead and improve performance, but will result in new data messages longer to be searchable in Graylog.</>}
                                         value={moment.duration(value, 'milliseconds').as(fieldTypeRefreshIntervalUnit)}
                                         unit={fieldTypeRefreshIntervalUnit.toUpperCase()}
                                         units={['SECONDS', 'MINUTES']}
                                         required
                                         update={(intervalValue: number, unit: Unit) => onFieldTypeRefreshIntervalChange(
                                           intervalValue, unit, name, onChange, setFieldValue,
                                         )} />
                        )}
                      </Field>
                    </HideOnCloud>
                  </Section>

                  <Section title="Rotation & Retention">
                    {isCloud && !enableDataTieringCloud ? (
                      <>
                        {indexSet.writable && <RotationStrategies rotationStrategies={rotationStrategies} indexSetRotationStrategy={values.rotation_strategy} indexSetRotationStrategyClass={values.rotation_strategy_class} />}
                        {indexSet.writable && <RetentionConfig retentionStrategies={retentionStrategies} retentionStrategiesContext={retentionStrategiesContext} indexSetRetentionStrategy={values.retention_strategy} IndexSetRetentionStrategyClass={values.retention_strategy_class} />}
                      </>
                    ) : (
                      <>
                        <SegmentedControl<RetentionConfigSegment> data={retentionConfigSegments}
                                                                  value={selectedRetentionSegment}
                                                                  onChange={setSelectedRetentionSegment} />

                        {selectedRetentionSegment === 'data_tiering' ? (
                          <>
                            <DataTieringVisualisation minDays={values.data_tiering?.index_lifetime_min}
                                                      maxDays={values.data_tiering?.index_lifetime_max}
                                                      minDaysInHot={values.data_tiering?.index_hot_lifetime_min}
                                                      warmTierEnabled={values.data_tiering?.warm_tier_enabled}
                                                      archiveData={values.data_tiering?.archive_before_deletion} />
                            <DataTieringConfiguration />
                          </>
                        )
                          : (
                            <ConfigSegment>
                              {indexSet.writable && <RotationStrategies rotationStrategies={rotationStrategies} indexSetRotationStrategy={values.rotation_strategy} indexSetRotationStrategyClass={values.rotation_strategy_class} />}
                              {indexSet.writable && <RetentionConfig retentionStrategies={retentionStrategies} retentionStrategiesContext={retentionStrategiesContext} indexSetRetentionStrategy={values.retention_strategy} IndexSetRetentionStrategyClass={values.retention_strategy_class} />}
                            </ConfigSegment>
                          )}

                      </>
                    )}
                  </Section>
                  {isIndexFieldTypeChangeAllowed(indexSet) && (
                  <Section title="Field Type Profile">
                    <Field name="field_type_profile">
                      {({ field: { name, value } }) => (
                        <IndexSetProfileConfiguration value={value}
                                                      onChange={(profileId) => {
                                                        setFieldValue(name, profileId);
                                                      }}
                                                      name={name} />
                      )}
                    </Field>
                  </Section>
                  )}
                  <Section title="Important Note">
                    <Alert bsStyle="info">
                      These changes do not apply to any existing indices. They only apply to newly created indices.
                      To apply this to the current index set immediately, rotate the index.
                    </Alert>
                  </Section>
                  <SubmitWrapper>
                    <FormSubmit disabledSubmit={!isValid}
                                submitButtonText={submitButtonText}
                                submitLoadingText={submitLoadingText}
                                isSubmitting={isSubmitting}
                                isAsyncSubmit
                                displayCancel
                                onCancel={onCancel} />
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
