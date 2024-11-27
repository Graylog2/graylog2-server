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
import moment from 'moment';
import { useFormikContext } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormikInput } from 'components/common';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';
import type { DataTieringConfig, DataTieringFormValues } from 'components/indices/data-tiering';

const dayFields = ['index_lifetime_max', 'index_lifetime_min', 'index_hot_lifetime_min'];
const hotWarmOnlyFormFields = ['index_hot_lifetime_min', 'warm_tier_enabled', 'warm_tier_repository_name'];

const DATA_TIERING_HOT_ONLY_DEFAULTS = {
  index_lifetime_max: 40,
  index_lifetime_min: 30,
};

const DATA_TIERING_HOT_WARM_DEFAULTS = {
  archive_before_deletion: false,
  warm_tier_enabled: false,
  index_hot_lifetime_min: 10,
  warm_tier_repository_name: null,
};

export const durationToRoundedDays = (duration: string) => Math.round(moment.duration(duration).asDays());

const dataTieringFormValuesWithDefaults = (values: DataTieringFormValues, pluginStore) : DataTieringFormValues => {
  const dataTieringPlugin = pluginStore.exports('dataTiering').find((plugin) => (plugin.type === DATA_TIERING_TYPE.HOT_WARM));
  const dataTieringType = dataTieringPlugin?.type ?? DATA_TIERING_TYPE.HOT_ONLY;

  if (dataTieringType === DATA_TIERING_TYPE.HOT_WARM) {
    const hotWarmDefaults = { ...DATA_TIERING_HOT_ONLY_DEFAULTS, ...DATA_TIERING_HOT_WARM_DEFAULTS, ...values };

    return hotWarmDefaults;
  }

  const hotOnlyDefaults = { ...DATA_TIERING_HOT_ONLY_DEFAULTS, ...values };

  return hotOnlyDefaults;
};

export const prepareDataTieringInitialValues = (config: DataTieringConfig, pluginStore) : DataTieringFormValues => {
  let formValues = { ...config };

  dayFields.forEach((field) => {
    if (formValues[field]) {
      const numberValue = durationToRoundedDays(formValues[field]);
      formValues = { ...formValues, [field]: numberValue };
    }
  });

  return dataTieringFormValuesWithDefaults(formValues as unknown as DataTieringFormValues, pluginStore);
};

export const prepareDataTieringConfig = (formValues: DataTieringFormValues, pluginStore) : DataTieringConfig => {
  const dataTieringPlugin = pluginStore.exports('dataTiering').find((plugin) => (plugin.type === DATA_TIERING_TYPE.HOT_WARM));
  const dataTieringType = dataTieringPlugin?.type ?? DATA_TIERING_TYPE.HOT_ONLY;

  let config = dataTieringFormValuesWithDefaults(formValues, pluginStore);

  if (dataTieringType === DATA_TIERING_TYPE.HOT_ONLY) {
    hotWarmOnlyFormFields.forEach((field) => {
      delete config[field];
    });
  }

  dayFields.forEach((field) => {
    if (config[field]) {
      config = { ...config, [field]: `P${config[field]}D` };
    }
  });

  return { ...config, type: dataTieringType } as unknown as DataTieringConfig;
};

type DataTiering = {
  data_tiering: DataTieringFormValues
}

type FormValues<T extends string | undefined> = T extends undefined ? DataTiering : T extends string ? { [Key in T]: DataTiering } : never

const DataTieringConfiguration = <ValuesPrefix extends string | undefined, >({ valuesPrefix } : { valuesPrefix?: ValuesPrefix }) => {
  const dataTieringPlugin = PluginStore.exports('dataTiering').find((plugin) => (plugin.type === 'hot_warm'));

  const { values } = useFormikContext<FormValues<ValuesPrefix>>();

  const formValue = (field: keyof DataTieringFormValues) => {
    if (valuesPrefix) {
      return values[valuesPrefix as string]?.data_tiering?.[field];
    }

    return values?.data_tiering?.[field];
  };

  const fieldName = (field: keyof DataTieringFormValues) => {
    if (valuesPrefix) {
      return `${valuesPrefix}.data_tiering.${field}`;
    }

    return `data_tiering.${field}`;
  };

  const validateMaxDaysInStorage = (value) => {
    if (value < 0) {
      return 'Negative numbers are not allowed.';
    }

    return '';
  };

  const validateMinDaysInStorage = (value) => {
    const errors = [];

    if (value < 0) {
      errors.push('Negative numbers are not allowed.');
    }

    if (value > formValue('index_lifetime_max')) {
      errors.push('Min. days in storage needs to be smaller than max. days in storage.');
    }

    if (formValue('warm_tier_enabled') && value < formValue('index_hot_lifetime_min')) {
      errors.push('Min. days in storage needs to be bigger than min. days in hot tier.');
    }

    if (errors.length > 0) {
      return errors.join(' ');
    }

    return '';
  };

  return (
    <>
      <FormikInput type="number"
                   id="data-tiering-index-lifetime-max"
                   label="Max. days in storage"
                   name={fieldName('index_lifetime_max')}
                   min={0}
                   help="After how many days your data should be deleted."
                   validate={validateMaxDaysInStorage}
                   required />
      <FormikInput type="number"
                   id="data-tiering-index-lifetime-min"
                   label="Min. days in storage"
                   name={fieldName('index_lifetime_min')}
                   min={0}
                   max={formValue('index_lifetime_max')}
                   validate={validateMinDaysInStorage}
                   help="How many days at minumum your data should be stored."
                   required />

      {dataTieringPlugin && (
        <>
          <FormikInput type="checkbox"
                       id="data_tiering-archive-before-deletion"
                       label="Archive before deletion"
                       name={fieldName('archive_before_deletion')}
                       help="Archive this index before it is deleted?" />
          <dataTieringPlugin.TiersConfigurationFields valuesPrefix={valuesPrefix} />
        </>
      )}
    </>
  );
};

export default DataTieringConfiguration;
