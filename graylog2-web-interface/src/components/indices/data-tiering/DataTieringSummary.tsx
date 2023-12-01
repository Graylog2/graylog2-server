import React from 'react';

import type { DataTieringConfig } from 'components/indices/data-tiering';

type Props = {
  config: DataTieringConfig
}

const DataTieringSummary = ({ config } : Props) => (
  <div>
    <dl>
      <dt>Rotation strategy:</dt>
      <dd>Data Tiering</dd>
      <dt>Max # days in storage:</dt>
      <dd>{config.index_lifetime_max}</dd>
      <dt>Min # days in storage:</dt>
      <dd>{config.index_lifetime_min}</dd>
    </dl>
  </div>
);

export default DataTieringSummary;
