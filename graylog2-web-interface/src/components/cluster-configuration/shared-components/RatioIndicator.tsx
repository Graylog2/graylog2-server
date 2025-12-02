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
import styled from 'styled-components';

import NumberUtils from 'util/NumberUtils';

import { StyledLabel } from './NodeMetricsLayout';

const SecondaryText = styled.div`
  font-size: small;

  span {
    font-size: inherit;
  }
`;

export const computeRatio = (used: number | undefined | null, max: number | undefined | null) => {
  if (used == null || max == null || max === 0) {
    return undefined;
  }

  return used / max;
};

type Props = {
  ratio: number | undefined | null;
  warningThreshold?: number;
  dangerThreshold?: number;
};

const RatioIndicator = ({ ratio, warningThreshold = Number.NaN, dangerThreshold = Number.NaN }: Props) => {
  if (ratio === undefined || ratio === null) {
    return null;
  }

  const formattedRatio = NumberUtils.formatPercentage(ratio);
  const exceedsDanger = !Number.isNaN(dangerThreshold) && ratio >= dangerThreshold;
  const exceedsWarning = !exceedsDanger && !Number.isNaN(warningThreshold) && ratio >= warningThreshold;

  if (!exceedsDanger && !exceedsWarning) {
    return (
      <SecondaryText>
        <span>{formattedRatio}</span>
      </SecondaryText>
    );
  }

  return (
    <SecondaryText>
      <StyledLabel bsStyle={exceedsDanger ? 'danger' : 'warning'} bsSize="xs">
        {formattedRatio}
      </StyledLabel>
    </SecondaryText>
  );
};

export const buildRatioIndicator = (
  ratio: number | undefined | null,
  warningThreshold?: number,
  dangerThreshold?: number,
) =>
  ratio == null ? null : (
    <RatioIndicator ratio={ratio} warningThreshold={warningThreshold} dangerThreshold={dangerThreshold} />
  );

export default RatioIndicator;
