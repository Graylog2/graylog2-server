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
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { TimeRange, AbsoluteTimeRange } from 'views/logic/queries/Query';
import { Icon } from 'components/common';

import AbsoluteRangeField from './AbsoluteRangeField';

type Props = {
  disabled: boolean,
  originalTimeRange: TimeRange,
  limitDuration: number,
  currentTimerange: AbsoluteTimeRange,
};

const AbsoluteWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: stretch;
  justify-content: space-around;
`;

const RangeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 4;
  align-items: center;
  min-height: 290px;
  display: flex;
  flex-direction: column;
  
  .DayPicker-wrapper {
    padding-bottom: 0;
  }
`;

const IconWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 0.75;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const AbsoluteTimeRangeSelector = ({ disabled, limitDuration, originalTimeRange, currentTimerange }: Props) => {
  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <AbsoluteRangeField from
                            originalTimeRange={originalTimeRange}
                            disabled={disabled}
                            limitDuration={limitDuration}
                            currentTimerange={currentTimerange} />
      </RangeWrapper>

      <IconWrap>
        <Icon name="arrow-right" />
      </IconWrap>

      <RangeWrapper>
        <AbsoluteRangeField from={false}
                            originalTimeRange={originalTimeRange}
                            disabled={disabled}
                            limitDuration={limitDuration}
                            currentTimerange={currentTimerange} />
      </RangeWrapper>
    </AbsoluteWrapper>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
  limitDuration: PropTypes.number,
  currentTimerange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
  limitDuration: 0,
};

export default AbsoluteTimeRangeSelector;
