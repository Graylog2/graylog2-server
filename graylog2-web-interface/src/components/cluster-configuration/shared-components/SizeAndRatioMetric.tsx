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

import NumberUtils from 'util/NumberUtils';

import { buildRatioIndicator, computeRatio } from './RatioIndicator';
import { MetricPlaceholder, MetricsColumn, MetricsRow } from './NodeMetricsLayout';

type Props = {
  used: number | undefined | null;
  max: number | undefined | null;
  warningThreshold?: number;
  dangerThreshold?: number;
  ratio?: number | undefined | null;
  ratioPercent?: number | undefined | null;
};

const formatBytes = (value: number | undefined | null) => (value == null ? '' : NumberUtils.formatBytes(value));

const SizeAndRatioMetric = ({
  used,
  max,
  warningThreshold = undefined,
  dangerThreshold = undefined,
  ratio = undefined,
  ratioPercent = undefined,
}: Props) => {
  const usedLabel = formatBytes(used);
  const maxLabel = formatBytes(max);
  const sizeLabel = [usedLabel, maxLabel].filter(Boolean).join(' / ');
  const ratioFromPercent = ratioPercent == null ? undefined : ratioPercent / 100;
  const effectiveRatio = ratio ?? ratioFromPercent ?? computeRatio(used, max);
  const ratioIndicator = buildRatioIndicator(effectiveRatio, warningThreshold, dangerThreshold);

  if (!sizeLabel && !ratioIndicator) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      {sizeLabel && (
        <MetricsRow>
          <span>{sizeLabel}</span>
        </MetricsRow>
      )}
      {ratioIndicator && <MetricsRow>{ratioIndicator}</MetricsRow>}
    </MetricsColumn>
  );
};

export default SizeAndRatioMetric;
