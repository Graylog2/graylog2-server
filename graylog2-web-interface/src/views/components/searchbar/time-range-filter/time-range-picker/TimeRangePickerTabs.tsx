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
import { useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

import { availableTimeRangeTypes } from 'views/Constants';
import { Tab, Tabs } from 'components/bootstrap';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  NoTimeRangeOverride,
  TimeRange,
  RelativeTimeRange,
} from 'views/logic/queries/Query';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import type { SupportedTimeRangeType } from './TimeRangePicker';

export const timeRangePickerTabs = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

type TimeRangeTabsArguments = {
  activeTab: SupportedTimeRangeType,
  limitDuration: number,
  setValidatingKeyword: (status: boolean) => void,
  tabs: Array<SupportedTimeRangeType>,
}

const StyledTabs = styled(Tabs)`
  margin-top: 1px;
  margin-bottom: 9px;
`;

const timeRangeTypeTabs = ({
  activeTab,
  limitDuration,
  setValidatingKeyword,
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
                            limitDuration={limitDuration}
                            setValidatingKeyword={type === 'keyword' ? setValidatingKeyword : undefined} />
        )}
      </Tab>
    );
  });

type Props = {
  handleActiveTab: (nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => void,
  currentTimeRange: NoTimeRangeOverride | TimeRange,
  limitDuration: number,
  validTypes: Array<'absolute' | 'relative' | 'keyword'>,
  setValidatingKeyword: (validating: boolean) => void,
};

const TimeRangeTabs = ({
  handleActiveTab,
  currentTimeRange,
  limitDuration,
  validTypes,
  setValidatingKeyword,
}: Props) => {
  const [activeTab, setActiveTab] = useState('type' in currentTimeRange ? currentTimeRange.type : undefined);
  const sendTelemetry = useSendTelemetry();

  const onSelect = useCallback((nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => {
    handleActiveTab(nextTab);
    setActiveTab(nextTab);

    sendTelemetry('click', {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'search-time-range',
      event_details: {
        tab: nextTab,
      },
    });
  }, [handleActiveTab, sendTelemetry]);

  const tabs = useMemo(() => timeRangeTypeTabs({
    activeTab,
    limitDuration,
    setValidatingKeyword,
    tabs: validTypes,
  }), [activeTab, limitDuration, setValidatingKeyword, validTypes]);

  return (
    <StyledTabs id="dateTimeTypes"
                defaultActiveKey={availableTimeRangeTypes[0].type}
                activeKey={activeTab ?? -1}
                onSelect={onSelect}
                animation={false}>
      {tabs}
      {!activeTab && (<TabDisabledTimeRange />)}
    </StyledTabs>
  );
};

export default TimeRangeTabs;
