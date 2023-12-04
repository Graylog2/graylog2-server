import React from 'react';

import type { DataTieringConfig } from 'components/indices/data-tiering';
import { durationToRoundedDays } from 'components/indices/data-tiering';

type Props = {
  config: DataTieringConfig
}

const DataTieringSummary = ({ config } : Props) => (
  <div>
    <dl>
      <dt>Rotation strategy:</dt>
      <dd>Data Tiering</dd>
      <dt>Max. in storage:</dt>
      <dd>{durationToRoundedDays(config.index_lifetime_max)} days</dd>
      <dt>Min. in storage:</dt>
      <dd>{durationToRoundedDays(config.index_lifetime_min)} days</dd>
    </dl>
  </div>
);

export default DataTieringSummary;
