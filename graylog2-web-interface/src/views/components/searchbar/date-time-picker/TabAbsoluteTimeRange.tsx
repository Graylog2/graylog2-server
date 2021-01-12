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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import moment from 'moment';

import { TimeRange, AbsoluteTimeRange } from 'views/logic/queries/Query';
import { Icon } from 'components/common';
import { Accordion, AccordionItem } from 'components/graylog';

import AbsoluteTimestamp from './AbsoluteTimestamp';
import AbsoluteCalendar from './AbsoluteCalendar';

type Props = {
  disabled: boolean,
  originalTimeRange: TimeRange,
  limitDuration: number,
  currentTimeRange: AbsoluteTimeRange,
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

const TabAbsoluteTimeRange = ({ disabled, limitDuration, currentTimeRange }: Props) => {
  const [activeAccordion, setActiveAccordion] = useState();
  const toStartDate = moment(currentTimeRange.from).toDate();
  const fromStartDate = limitDuration ? moment().seconds(-limitDuration).toDate() : null;

  const handleSelect = (nextKey) => {
    setActiveAccordion(nextKey ?? activeAccordion);
  };

  return (
    <AbsoluteWrapper>
      <StyledAccordion defaultActiveKey="calendar"
                       onSelect={handleSelect}
                       id="absolute-time-ranges"
                       activeKey={activeAccordion}>

        <AccordionItem name="Calendar">
          <RangeWrapper>
            <AbsoluteCalendar disabled={disabled}
                              startDate={fromStartDate}
                              currentTimeRange={currentTimeRange}
                              range="from" />

          </RangeWrapper>

          <IconWrap>
            <Icon name="arrow-right" />
          </IconWrap>

          <RangeWrapper>
            <AbsoluteCalendar disabled={disabled}
                              startDate={toStartDate}
                              currentTimeRange={currentTimeRange}
                              range="to" />
          </RangeWrapper>
        </AccordionItem>

        <AccordionItem name="Timestamp">
          <TimestampContent>
            <p>Date should be formatted as <code>YYYY-MM-DD [HH:mm:ss[.SSS]]</code>.</p>
            <FlexWrap>
              <RangeWrapper>
                <AbsoluteTimestamp disabled={disabled}
                                   currentTimeRange={currentTimeRange}
                                   range="from" />
              </RangeWrapper>

              <IconWrap>
                <Icon name="arrow-right" />
              </IconWrap>

              <RangeWrapper>
                <AbsoluteTimestamp disabled={disabled}
                                   currentTimeRange={currentTimeRange}
                                   range="to" />
              </RangeWrapper>
            </FlexWrap>
          </TimestampContent>
        </AccordionItem>
      </StyledAccordion>

    </AbsoluteWrapper>
  );
};

TabAbsoluteTimeRange.propTypes = {
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
  limitDuration: PropTypes.number,
  currentTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
};

TabAbsoluteTimeRange.defaultProps = {
  disabled: false,
  limitDuration: 0,
};

export default TabAbsoluteTimeRange;
