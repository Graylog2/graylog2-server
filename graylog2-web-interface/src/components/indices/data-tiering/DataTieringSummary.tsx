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
