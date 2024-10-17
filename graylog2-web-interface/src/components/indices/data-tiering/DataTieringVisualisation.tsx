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
import React, { useState } from 'react';
import styled, { css, useTheme } from 'styled-components';

import { Tooltip } from 'components/bootstrap';

type Props = {
  minDays?: number,
  maxDays?: number,
  minDaysInHot?: number,
  warmTierEnabled?: boolean,
  archiveData?: boolean,
}

type BarProps = {
  value: number,
  color: string
}

type LabelProps = {
  value: number,
}

const PERCENTAGE_SPACING_THRESHOLD = 20;

const VisualisationWrapper = styled.div(({ theme }) => css`
  padding-left: ${theme.spacings.md};
  padding-right: ${theme.spacings.md};
  display: flex;
  align-items: center;
  justify-content: space-between;
`);

const BarWrapper = styled.div`
  overflow: hidden;
  width: 100%;
`;

const MaxDaysLabel = styled.p(({ theme }) => css`
  margin-left: ${theme.spacings.sm};
  margin-bottom: 0;
`);

const AnnotationBar = styled.div`
  position: relative;
  height: 60px;
`;

const LifeCycleBar = styled.div(({ theme }) => css`
  height: 40px;
  background: ${theme.colors.variant.lighter.info};
  position: relative;
`);

const Bar = styled.div<BarProps>(({ theme, value, color }) => css`
    height: 100%;
    font-size: ${theme.fonts.size.small};
    text-align: center;
    flex-shrink: 0;
    background-color: ${color};
    transition: width 500ms ease-in-out;
    position: absolute;
    left: 0;
    width: ${value}%;
    max-width: 100%;
    cursor: pointer;

    &::after {
      border-right: 1px solid ${theme.colors.variant.darkest.info};
      content: '';
      position: absolute;
      right: 0;
      bottom: 0;
      height: 60px;
      z-index: 1;
    }
`);

const Label = styled.div<LabelProps>(({ value }) => css`
  position: absolute;
  bottom: 25px;
  ${value > 3
    ? `right: ${100 - value}%;`
    : 'left: 0;'}
`);

const StyledTooltip = styled(Tooltip)<{ value: number }>(({ value }) => css`
  position: absolute;
  top: 0;
  
  ${value > PERCENTAGE_SPACING_THRESHOLD
    ? `
  right: ${100 - value}%;

    &.bottom > .tooltip-arrow {
      margin-left: -20px;
    }
  `
    : `
  left: 0;
  margin-left: 3px;
  `}    
`);

const DataTieringVisualisation = ({ archiveData = false, minDays = 0, maxDays = 0, minDaysInHot = 0, warmTierEnabled = false }: Props) => {
  const [showMinDaysTooltip, setShowMinDaysTooltip] = useState<boolean>(false);
  const [showMinDaysInHotTooltip, setShowMinDaysInHotTooltip] = useState<boolean>(false);
  const theme = useTheme();

  const percentageFor = (days: number) => {
    if (days <= 0 || maxDays <= 0) return 0;

    return ((days / maxDays) * 100);
  };

  const minDaysPercentage = percentageFor(minDays);
  const minDaysInHotPercentage = percentageFor(minDaysInHot);

  const showHotTier = warmTierEnabled && minDaysInHotPercentage > 0;

  return (
    <VisualisationWrapper>
      <BarWrapper>
        <AnnotationBar>
          {minDaysPercentage > 0 && (<Label value={minDaysPercentage}>{minDays} days</Label>)}
          {showHotTier && (minDaysInHotPercentage !== minDaysPercentage) && (
            <Label value={minDaysInHotPercentage}>{minDaysInHot} days</Label>
          )}
        </AnnotationBar>

        <LifeCycleBar>
          <Bar value={minDaysPercentage}
               color={theme.colors.variant.info}
               onMouseEnter={() => setShowMinDaysTooltip(true)}
               onMouseLeave={() => setShowMinDaysTooltip(false)} />
          {showHotTier && (
            <Bar value={minDaysInHotPercentage}
                 color={theme.colors.variant.darkest.info}
                 onMouseEnter={() => setShowMinDaysInHotTooltip(true)}
                 onMouseLeave={() => setShowMinDaysInHotTooltip(false)} />
          )}
        </LifeCycleBar>
        <AnnotationBar>
          {showHotTier && showMinDaysInHotTooltip && (
            minDaysInHotPercentage === minDaysPercentage ? (
              <StyledTooltip placement="bottom"
                             id="min-days-in-hot-and-storage"
                             arrowOffsetLeft={minDaysInHotPercentage <= PERCENTAGE_SPACING_THRESHOLD ? '10px' : '100%'}
                             value={minDaysInHotPercentage}>
                Min. # of days in Hot Tier and storage
              </StyledTooltip>
            ) : (
              <StyledTooltip placement="bottom"
                             id="min-days-in-hot"
                             arrowOffsetLeft={minDaysInHotPercentage <= PERCENTAGE_SPACING_THRESHOLD ? '10px' : '100%'}
                             value={minDaysInHotPercentage}>
                Min. # of days in Hot Tier
              </StyledTooltip>
            )
          )}
          {minDaysPercentage > 0 && showMinDaysTooltip && (
            <StyledTooltip placement="bottom"
                           id="min-days-in-storage"
                           arrowOffsetLeft={minDaysPercentage <= PERCENTAGE_SPACING_THRESHOLD ? '10px' : '100%'}
                           value={minDaysPercentage}>
              Min. # of days in storage
            </StyledTooltip>
          )}
        </AnnotationBar>
      </BarWrapper>
      <MaxDaysLabel>{archiveData ? 'Archived and deleted' : 'Deleted'} after <strong>{maxDays} days</strong></MaxDaysLabel>
    </VisualisationWrapper>
  );
};

export default DataTieringVisualisation;
