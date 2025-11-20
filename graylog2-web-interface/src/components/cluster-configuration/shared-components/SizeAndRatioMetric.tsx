import React from 'react';

import NumberUtils from 'util/NumberUtils';

import { buildRatioIndicator, computeRatio } from './RatioIndicator';
import { MetricsColumn, MetricsRow } from './NodeMetricsLayout';

type Props = {
  used: number | undefined | null;
  max: number | undefined | null;
  warningThreshold: number;
  dangerThreshold: number;
  ratio?: number | undefined | null;
  ratioPercent?: number | undefined | null;
};

const formatBytes = (value: number | undefined | null) => (value == null ? '' : NumberUtils.formatBytes(value));

const SizeAndRatioMetric = ({ used, max, warningThreshold, dangerThreshold, ratio = undefined, ratioPercent = undefined }: Props) => {
  const usedLabel = formatBytes(used);
  const maxLabel = formatBytes(max);
  const sizeLabel = [usedLabel, maxLabel].filter(Boolean).join(' / ');
  const ratioFromPercent = ratioPercent == null ? undefined : ratioPercent / 100;
  const effectiveRatio = ratio ?? ratioFromPercent ?? computeRatio(used, max);
  const ratioIndicator = buildRatioIndicator(effectiveRatio, warningThreshold, dangerThreshold);

  if (!sizeLabel && !ratioIndicator) {
    return null;
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
