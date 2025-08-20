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
