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

import RatioIndicator from '../../shared-components/RatioIndicator';
import { MetricPlaceholder, MetricsColumn, MetricsRow } from '../../shared-components/NodeMetricsLayout';

type Props = {
  percent: number | undefined | null;
  warningThreshold: number;
  dangerThreshold: number;
};

const PercentRatioCell = ({ percent, warningThreshold, dangerThreshold }: Props) => {
  if (percent == null) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      <MetricsRow>
        <RatioIndicator ratio={percent / 100} warningThreshold={warningThreshold} dangerThreshold={dangerThreshold} />
      </MetricsRow>
    </MetricsColumn>
  );
};

export default PercentRatioCell;
