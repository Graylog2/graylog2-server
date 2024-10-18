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
import { useState } from 'react';
import styled from 'styled-components';
import moment from 'moment';
import { useFormikContext } from 'formik';

import { Icon, Accordion, AccordionItem } from 'components/common';
import useUserDateTime from 'hooks/useUserDateTime';

import AbsoluteCalendar from './AbsoluteCalendar';
import AbsoluteTimestamp from './AbsoluteTimestamp';
import type { TimeRangePickerFormValues } from './TimeRangePicker';

type Props = {
  disabled?: boolean
  limitDuration?: number
};

const AbsoluteWrapper = styled.div`
  display: flex;
  align-items: stretch;
  justify-content: space-around;
`;

const RangeWrapper = styled.div`
  flex: 4;
  align-items: center;
  display: flex;
  flex-direction: column;
  
  .DayPicker-wrapper {
    padding-bottom: 0;
  }
`;

const IconWrap = styled.div`
  flex: 0.75;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateY(-0.65em);
`;

const StyledAccordion = styled(Accordion)`
  width: 100%;
  
  .panel-body {
    display: flex;
  }
`;

const TimestampContent = styled.div`
  flex: 1;
`;

const FlexWrap = styled.div`
  display: flex;
`;

const TabAbsoluteTimeRange = ({ disabled = false, limitDuration = 0 }: Props) => {
  const { values: { timeRangeTabs } } = useFormikContext<TimeRangePickerFormValues>();
  const activeTabTimeRange = timeRangeTabs.absolute;

  const { toUserTimezone } = useUserDateTime();
  const [activeAccordion, setActiveAccordion] = useState<'Timestamp' | 'Calendar' | undefined>();
  const toStartDate = moment(activeTabTimeRange.from).toDate();
  const fromStartDate = limitDuration ? toUserTimezone(new Date()).seconds(-limitDuration).toDate() : undefined;

  const handleSelect = (nextKey: 'Timestamp' | 'Calendar' | undefined) => {
    setActiveAccordion(nextKey ?? activeAccordion);
  };

  return (
    <AbsoluteWrapper>
      <StyledAccordion defaultActiveKey="calendar"
                       onSelect={handleSelect}
                       id="absolute-time-ranges"
                       data-testid="absolute-time-ranges"
                       activeKey={activeAccordion}>

        <AccordionItem name="Calendar">
          <RangeWrapper>
            <AbsoluteCalendar startDate={fromStartDate}
                              timeRange={activeTabTimeRange}
                              range="from" />

          </RangeWrapper>

          <IconWrap>
            <Icon name="arrow_right_alt" />
          </IconWrap>

          <RangeWrapper>
            <AbsoluteCalendar startDate={toStartDate}
                              timeRange={activeTabTimeRange}
                              range="to" />
          </RangeWrapper>
        </AccordionItem>

        <AccordionItem name="Timestamp">
          <TimestampContent>
            <p>Date should be formatted as <code>YYYY-MM-DD [HH:mm:ss[.SSS]]</code>.</p>
            <FlexWrap>
              <RangeWrapper>
                <AbsoluteTimestamp disabled={disabled}
                                   timeRange={activeTabTimeRange}
                                   range="from" />
              </RangeWrapper>

              <IconWrap>
                <Icon name="arrow_right_alt" />
              </IconWrap>

              <RangeWrapper>
                <AbsoluteTimestamp disabled={disabled}
                                   timeRange={activeTabTimeRange}
                                   range="to" />
              </RangeWrapper>
            </FlexWrap>
          </TimestampContent>
        </AccordionItem>
      </StyledAccordion>
    </AbsoluteWrapper>
  );
};

export default TabAbsoluteTimeRange;
