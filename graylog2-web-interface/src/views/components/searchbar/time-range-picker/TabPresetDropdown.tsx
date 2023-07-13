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

import TimerangeSelector from 'views/components/searchbar/TimerangeSelector';
import RangePresetDropdown from 'views/components/searchbar/time-range-preset-select/TimeRangePresetDropdown';
import type { TimeRange } from 'views/logic/queries/Query';
import type { QuickAccessTimeRange } from 'components/configurations/QuickAccessTimeRangeForm';

const ConfiguredWrapper = styled.div`
  display: flex;
  margin: 3px 0;
  justify-content: end;
`;

type Props = {
  disabled: boolean,
  onSetPreset: (timerange: TimeRange) => void,
  availableOptions: Array<QuickAccessTimeRange>,
};
const TabPresetDropdown = ({ onSetPreset, availableOptions, disabled }: Props) => (
  <ConfiguredWrapper>
    <TimerangeSelector className="relative">
      <RangePresetDropdown disabled={disabled}
                           onChange={onSetPreset}
                           availableOptions={availableOptions} />
    </TimerangeSelector>
  </ConfiguredWrapper>
);

export default TabPresetDropdown;
