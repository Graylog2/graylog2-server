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

import { DataTieringVisualisation, DataTieringConfiguration } from 'components/indices/data-tiering';
import type { IndexSetFormValues } from 'stores/indices/IndexSetsStore';

type Props = {
  values: IndexSetFormValues;
  hiddenFields?: string[];
  immutableFields?: string[];
  hasFieldRestrictionPermission: boolean;
};

const IndexSetRotationRetentionDataTieringConfiguration = ({
  values,
  hiddenFields = [],
  immutableFields = [],
  hasFieldRestrictionPermission,
}: Props) => (
  <>
    <DataTieringVisualisation
      minDays={values.data_tiering?.index_lifetime_min}
      maxDays={values.data_tiering?.index_lifetime_max}
      minDaysInHot={values.data_tiering?.index_hot_lifetime_min}
      warmTierEnabled={values.data_tiering?.warm_tier_enabled}
      archiveData={values.data_tiering?.archive_before_deletion}
    />
    <DataTieringConfiguration
      hiddenFields={hiddenFields}
      immutableFields={immutableFields}
      hasFieldRestrictionPermission={hasFieldRestrictionPermission}
    />
  </>
);
export default IndexSetRotationRetentionDataTieringConfiguration;
