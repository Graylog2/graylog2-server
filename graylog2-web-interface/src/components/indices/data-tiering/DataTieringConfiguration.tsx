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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { useFormikContext } from 'formik';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { FormikFormGroup } from 'components/common';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';

const dayFields = ['index_lifetime_max', 'index_lifetime_min', 'index_hot_lifetime_min'];

export const prepareDataTieringInitialValues = (values: IndexSet) : IndexSet => {
  if (!values.data_tiering) return values;

  let { data_tiering } = values;

  dayFields.forEach((field) => {
    if (data_tiering[field]) {
      const numberValue = parseInt(data_tiering[field].substring(1, data_tiering[field].length() - 1), 10);
      data_tiering = { ...data_tiering, [field]: numberValue };
    }
  });

  return { ...values, data_tiering };
};

export const prepareDataTieringConfig = (values: IndexSet) : IndexSet => {
  if (!values.data_tiering) return values;

  const defaultType = 'hot_only';
  const dataTieringPlugin = PluginStore.exports('dataTiering').find((plugin) => (plugin.type === DATA_TIERING_TYPE.HOT_WARM));
  const dataTieringType = dataTieringPlugin?.type ?? defaultType;

  let { data_tiering } = values;

  dayFields.forEach((field) => {
    if (data_tiering[field]) {
      data_tiering = { ...data_tiering, [field]: `P${data_tiering[field]}D` };
    }
  });

  data_tiering = { ...data_tiering, type: dataTieringType };

  return { ...values, data_tiering };
};

const DataTieringConfiguration = () => {
  const dataTieringPlugin = PluginStore.exports('dataTiering').find((plugin) => (plugin.type === 'hot_warm'));

  const { values } = useFormikContext<IndexSet>();

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

    if (value > values?.data_tiering?.index_lifetime_max) {
      errors.push('Min. days in storage needs to be smaller than max. days in storage.');
    }

    if (values?.data_tiering?.warm_tier_enabled && value < values?.data_tiering?.index_hot_lifetime_min) {
      errors.push('Min. days in storage needs to be bigger than min. days in hot tier.');
    }

    if (errors.length > 0) {
      return errors.join(' ');
    }

    return '';
  };

  return (
    <>
      <FormikFormGroup type="number"
                       label="Max. days in storage"
                       name="data_tiering.index_lifetime_max"
                       min={0}
                       help="After how many days your data should be deleted."
                       validate={validateMaxDaysInStorage}
                       required />
      <FormikFormGroup type="number"
                       label="Min. days in storage"
                       name="data_tiering.index_lifetime_min"
                       min={0}
                       max={values?.data_tiering?.index_lifetime_max}
                       validate={validateMinDaysInStorage}
                       help="How many days at minumum your data should be stored."
                       required />
      {dataTieringPlugin && <dataTieringPlugin.TiersConfigurationFields />}
    </>
  );
};

export default DataTieringConfiguration;
