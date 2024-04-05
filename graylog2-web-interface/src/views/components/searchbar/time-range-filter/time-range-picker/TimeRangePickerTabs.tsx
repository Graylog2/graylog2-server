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
import { useCallback, useMemo } from 'react';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { availableTimeRangeTypes } from 'views/Constants';
import { Tab, Tabs } from 'components/bootstrap';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  RelativeTimeRange,
} from 'views/logic/queries/Query';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { isTimeRange } from 'views/typeGuards/timeRange';
import migrateTimeRangeToNewType
  from 'views/components/searchbar/time-range-filter/time-range-picker/migrateTimeRangeToNewType';
import useUserDateTime from 'hooks/useUserDateTime';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { toDateObject } from 'util/DateTime';
import {
  RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
} from 'views/components/searchbar/time-range-filter/time-range-picker/RelativeTimeRangeClassifiedHelper';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { SelectCallback } from 'components/bootstrap/types';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import type { SupportedTimeRangeType, TimeRangePickerFormValues } from './TimeRangePicker';

export const timeRangePickerTabs = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

type TimeRangeTabsArguments = {
  activeTab: SupportedTimeRangeType,
  limitDuration: number,
  tabs: Array<SupportedTimeRangeType>,
}

const StyledTabs = styled(Tabs)`
  margin-top: 1px;
  margin-bottom: 9px;
`;

const timeRangeTypeTabs = ({
  activeTab,
  limitDuration,
  tabs,
}: TimeRangeTabsArguments) => availableTimeRangeTypes
  .filter(({ type }) => tabs.includes(type))
  .map(({ type, name }) => {
    const TimeRangeTypeTab = timeRangePickerTabs[type];

    return (
      <Tab title={name}
           key={`time-range-type-selector-${type}`}
           eventKey={type}>
        {type === activeTab && (
          <TimeRangeTypeTab disabled={false}
                            limitDuration={limitDuration} />
        )}
      </Tab>
    );
  });

const createDefaultRanges = (formatTime: (time: DateTime, format: DateTimeFormats) => string) => ({
  absolute: {
    type: 'absolute',
    from: formatTime(toDateObject(new Date()).subtract(300, 'seconds'), 'complete'),
    to: formatTime(toDateObject(new Date()), 'complete'),
  },
  relative: {
    type: 'relative',
    from: {
      value: 5,
      unit: 'minutes',
      isAllTime: false,
    },
    to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
  },
  keyword: {
    type: 'keyword',
    keyword: 'Last five minutes',
  },
  disabled: undefined,
});

type Props = {
  limitDuration: number,
  validTypes: Array<'absolute' | 'relative' | 'keyword'>,
};

const newTabTimeRange = ({
  activeTab,
  nextTab,
  timeRangeTabs,
  formatTime,
  defaultRanges,
  userTimezone,
}: {
  activeTab: TimeRangePickerFormValues['activeTab'],
  nextTab: TimeRangePickerFormValues['activeTab'],
  timeRangeTabs: TimeRangePickerFormValues['timeRangeTabs'],
  formatTime: (time: DateTime, format: DateTimeFormats) => string,
  defaultRanges: ReturnType<typeof createDefaultRanges>,
  userTimezone: string
}) => {
  if (timeRangeTabs[nextTab]) {
    return timeRangeTabs[nextTab];
  }

  if (isTimeRange(timeRangeTabs[activeTab])) {
    return migrateTimeRangeToNewType({
      oldTimeRange: timeRangeTabs[activeTab],
      type: nextTab,
      formatTime,
      userTimezone,
    });
  }

  return defaultRanges[nextTab];
};

const TimeRangeTabs = ({
  limitDuration,
  validTypes,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const { formatTime, userTimezone } = useUserDateTime();
  const { setValues, values: { activeTab, timeRangeTabs } } = useFormikContext<TimeRangePickerFormValues>();
  const defaultRanges = useMemo(() => createDefaultRanges(formatTime), [formatTime]);

  const onSelect = useCallback((nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => {
    setValues({
      timeRangeTabs: {
        ...timeRangeTabs,
        [nextTab]: newTabTimeRange({
          activeTab,
          nextTab,
          timeRangeTabs,
          formatTime,
          defaultRanges,
          userTimezone,
        }),
      },
      activeTab: nextTab,
    });

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_TAB_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'search-time-range',
      event_details: {
        tab: nextTab,
      },
    });
  }, [activeTab, defaultRanges, formatTime, location.pathname, sendTelemetry, setValues, timeRangeTabs, userTimezone]);

  const tabs = useMemo(() => timeRangeTypeTabs({
    activeTab,
    limitDuration,
    tabs: validTypes,
  }), [activeTab, limitDuration, validTypes]);

  return (
    <StyledTabs id="dateTimeTypes"
                defaultActiveKey={availableTimeRangeTypes[0].type}
                activeKey={activeTab ?? -1}
                onSelect={onSelect as SelectCallback}
                animation={false}>
      {tabs}
      {!activeTab && (<TabDisabledTimeRange />)}
    </StyledTabs>
  );
};

export default TimeRangeTabs;
