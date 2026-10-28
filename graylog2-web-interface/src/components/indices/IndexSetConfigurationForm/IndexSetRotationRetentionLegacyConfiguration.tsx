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
import React from 'react';
import { useTranslation } from 'react-i18next';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { IndexSet, IndexSetFormValues } from 'stores/indices/IndexSetsStore';
import type {
  RetentionStrategyContext,
  Strategies,
  RotationStrategyConfig,
  RetentionStrategyConfig,
} from 'components/indices/Types';
import { Spinner } from 'components/common';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';

type Props = {
  indexSet: IndexSet;
  values: IndexSetFormValues;
  retentionStrategies: Strategies;
  retentionStrategiesContext: RetentionStrategyContext;
  rotationStrategies: Strategies;
  hiddenFields: string[];
  immutableFields: string[];
  ignoreFieldRestrictions: boolean;
};

type RotationStrategiesProps = {
  rotationStrategies: Array<any>;
  indexSetRotationStrategy: RotationStrategyConfig;
  indexSetRotationStrategyClass: string;
  disabled?: boolean;
};

type RetentionConfigProps = {
  retentionStrategies: Array<any>;
  retentionStrategiesContext: RetentionStrategyContext;
  indexSetRetentionStrategy: RetentionStrategyConfig;
  IndexSetRetentionStrategyClass: string;
  hiddenFields?: string[];
  immutableFields?: string[];
  ignoreFieldRestrictions?: boolean;
  disabled?: boolean;
};

const _getRotationConfigState = (strategy: string, data: RotationStrategyConfig) => ({
  rotation_strategy_class: strategy,
  rotation_strategy: data,
});

const _getRetentionConfigState = (strategy: string, data: RetentionStrategyConfig) => ({
  retention_strategy_class: strategy,
  retention_strategy: data,
});

const RotationStrategies = ({
  rotationStrategies,
  indexSetRotationStrategy,
  indexSetRotationStrategyClass,
  disabled = false,
}: RotationStrategiesProps) => {
  const { t } = useTranslation('indexsets');

  if (!rotationStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration
      title={t('index_set_form.rotation_title')}
      name="rotation"
      description={t('index_set_form.rotation_description')}
      selectPlaceholder={t('index_set_form.rotation_placeholder')}
      label={t('index_set_form.rotation_label')}
      pluginExports={PluginStore.exports('indexRotationConfig')}
      strategies={rotationStrategies}
      activeConfig={{
        config: indexSetRotationStrategy,
        strategy: indexSetRotationStrategyClass,
      }}
      getState={_getRotationConfigState}
      disabled={disabled}
    />
  );
};

const RetentionConfig = ({
  retentionStrategies,
  retentionStrategiesContext,
  indexSetRetentionStrategy,
  IndexSetRetentionStrategyClass,
  disabled = false,
  hiddenFields = [],
  immutableFields = [],
  ignoreFieldRestrictions = false,
}: RetentionConfigProps) => {
  const { t } = useTranslation('indexsets');

  if (!retentionStrategies) return <Spinner />;

  return (
    <IndexMaintenanceStrategiesConfiguration
      title={t('index_set_form.retention_title')}
      name="retention"
      description={t('index_set_form.retention_description')}
      selectPlaceholder={t('index_set_form.retention_placeholder')}
      label={t('index_set_form.retention_label')}
      pluginExports={PluginStore.exports('indexRetentionConfig')}
      strategies={retentionStrategies}
      retentionStrategiesContext={retentionStrategiesContext}
      activeConfig={{
        config: indexSetRetentionStrategy,
        strategy: IndexSetRetentionStrategyClass,
      }}
      getState={_getRetentionConfigState}
      disabled={disabled}
      hiddenFields={hiddenFields}
      immutableFields={immutableFields}
      ignoreFieldRestrictions={ignoreFieldRestrictions}
    />
  );
};

const IndexSetRotationRetentionLegacyConfiguration = ({
  indexSet,
  values,
  rotationStrategies,
  retentionStrategies,
  retentionStrategiesContext,
  hiddenFields,
  immutableFields,
  ignoreFieldRestrictions,
}: Props) => {
  const sectionDisabled: boolean = immutableFields.includes('legacy');

  return (
    <>
      {indexSet.writable && (!hiddenFields?.includes('legacy.rotation_strategy') || ignoreFieldRestrictions) && (
        <RotationStrategies
          rotationStrategies={rotationStrategies}
          indexSetRotationStrategy={values.rotation_strategy}
          indexSetRotationStrategyClass={values.rotation_strategy_class}
          disabled={
            (immutableFields?.includes('legacy.rotation_strategy') || sectionDisabled) && !ignoreFieldRestrictions
          }
        />
      )}
      {indexSet.writable && (!hiddenFields?.includes('legacy.retention_strategy') || ignoreFieldRestrictions) && (
        <RetentionConfig
          retentionStrategies={retentionStrategies}
          retentionStrategiesContext={retentionStrategiesContext}
          indexSetRetentionStrategy={values.retention_strategy}
          IndexSetRetentionStrategyClass={values.retention_strategy_class}
          disabled={
            (immutableFields?.includes('legacy.retention_strategy') || sectionDisabled) && !ignoreFieldRestrictions
          }
          hiddenFields={hiddenFields}
          immutableFields={immutableFields}
          ignoreFieldRestrictions={ignoreFieldRestrictions}
        />
      )}
    </>
  );
};
export default IndexSetRotationRetentionLegacyConfiguration;
