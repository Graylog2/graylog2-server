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
import { useCallback, useContext } from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';

import TimeRangePresetDropdown from 'views/components/searchbar/time-range-filter/TimeRangePresetDropdown';
import { isTimeRange, isTypeRelative } from 'views/typeGuards/timeRange';
import { IfPermitted } from 'components/common';
import SaveTimeRangeAsPresetButton
  from 'views/components/searchbar/time-range-filter/time-range-picker/SaveTimeRangeAsPresetButton';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';
import type { TimeRange } from 'views/logic/queries/Query';
import type {
  TimeRangePickerFormValues,
} from 'views/components/searchbar/time-range-filter/time-range-picker/TimeRangePicker';
import {
  classifyRelativeTimeRange,
} from 'views/components/searchbar/time-range-filter/time-range-picker/RelativeTimeRangeClassifiedHelper';
import { ButtonToolbar } from 'components/bootstrap';

const Container = styled(ButtonToolbar)`
  float: right;
  margin-top: 6px;
`;

const normalizePresetTimeRange = (timeRange: TimeRange) => {
  if (isTypeRelative(timeRange)) {
    return classifyRelativeTimeRange(timeRange);
  }

  return timeRange;
};

const TimeRangePresetRow = () => {
  const { showAddToQuickListButton } = useContext(TimeRangeInputSettingsContext);
  const { showPresetsButton } = useContext(TimeRangeInputSettingsContext);
  const { values, setValues } = useFormikContext<TimeRangePickerFormValues>();
  const { activeTab, timeRangeTabs } = values;

  const onSetPreset = useCallback((newTimeRange: TimeRange) => {
    setValues({
      ...values,
      timeRangeTabs: {
        ...values.timeRangeTabs,
        [newTimeRange.type]: normalizePresetTimeRange(newTimeRange),
      },
      activeTab: newTimeRange.type,
    });
  }, [setValues, values]);

  return (
    <Container>
      {showAddToQuickListButton && isTimeRange(timeRangeTabs[activeTab]) && (
        <IfPermitted permissions="clusterconfigentry:edit">
          <SaveTimeRangeAsPresetButton />
        </IfPermitted>
      )}
      {showPresetsButton && <TimeRangePresetDropdown onChange={onSetPreset} />}
    </Container>
  );
};

export default TimeRangePresetRow;
