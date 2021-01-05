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
import styled, { css } from 'styled-components';
import { Field } from 'formik';
import moment from 'moment';

import { TimeRange, AbsoluteTimeRange } from 'views/logic/queries/Query';
import { Icon } from 'components/common';
import { Accordion, AccordionGroup } from 'components/graylog';

import AbsoluteDateInput from './AbsoluteDateInput';
import AbsoluteDatePicker from './AbsoluteDatePicker';
import AbsoluteTimeInput from './AbsoluteTimeInput';

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
`;

const StyledAccordionGroup = styled(AccordionGroup)`
  width: 100%;
  
  .panel-body {
    display: flex;
  }
`;

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px 3px 9px;
  height: 1.5em;
`);

const AbsoluteTimeRangeSelector = ({ disabled, limitDuration, currentTimeRange }: Props) => {
  const [activeTab, setActiveTab] = useState();
  const toStartDate = moment(currentTimeRange.from).toDate();
  const fromStartDate = limitDuration ? moment().seconds(-limitDuration).toDate() : toStartDate;

  return (
    <AbsoluteWrapper>
      <StyledAccordionGroup defaultActiveKey="text"
                            onSelect={(wat) => { setActiveTab(wat); }}
                            id="absolute-time-ranges"
                            activeKey={activeTab}>
        <Accordion name="Text">
          <RangeWrapper>
            <Field name="nextTimeRange['from']">
              {({ field: { value, onChange, name }, meta: { error } }) => {
                const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
                const dateTime = error ? currentTimeRange.from : value || currentTimeRange.from;

                return (
                  <>
                    <AbsoluteDateInput name={name}
                                       disabled={disabled}
                                       value={dateTime}
                                       onChange={_onChange} />

                    <ErrorMessage>{error ?? ' '}</ErrorMessage>
                  </>
                );
              }}
            </Field>
          </RangeWrapper>
          <IconWrap>
            <Icon name="arrow-right" />
          </IconWrap>
          <RangeWrapper>
            <Field name="nextTimeRange['to']">
              {({ field: { value, onChange, name }, meta: { error } }) => {
                const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
                const dateTime = error ? currentTimeRange.to : value || currentTimeRange.to;

                return (
                  <>
                    <AbsoluteDateInput name={name}
                                       disabled={disabled}
                                       value={dateTime}
                                       onChange={_onChange} />

                    <ErrorMessage>{error ?? ' '}</ErrorMessage>
                  </>
                );
              }}
            </Field>
          </RangeWrapper>
        </Accordion>

        <Accordion name="Calendar">
          <RangeWrapper>
            <Field name="nextTimeRange['from']">
              {({ field: { value, onChange, name }, meta: { error } }) => {
                const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
                const dateTime = error ? currentTimeRange.from : value || currentTimeRange.from;

                return (
                  <>
                    <AbsoluteDatePicker name={name}
                                        disabled={disabled}
                                        onChange={_onChange}
                                        startDate={fromStartDate}
                                        dateTime={dateTime} />

                    <AbsoluteTimeInput onChange={_onChange}
                                       range="from"
                                       dateTime={dateTime} />

                    <ErrorMessage>{error ?? ' '}</ErrorMessage>
                  </>
                );
              }}
            </Field>
          </RangeWrapper>
          <IconWrap>
            <Icon name="arrow-right" />
          </IconWrap>
          <RangeWrapper>
            <Field name="nextTimeRange['to']">
              {({ field: { value, onChange, name }, meta: { error } }) => {
                const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
                const dateTime = error ? currentTimeRange.to : value || currentTimeRange.to;

                return (
                  <>
                    <AbsoluteDatePicker name={name}
                                        disabled={disabled}
                                        onChange={_onChange}
                                        startDate={toStartDate}
                                        dateTime={dateTime} />

                    <AbsoluteTimeInput onChange={_onChange}
                                       range="to"
                                       dateTime={dateTime} />

                    <ErrorMessage>{error ?? ' '}</ErrorMessage>
                  </>
                );
              }}
            </Field>
          </RangeWrapper>
        </Accordion>
      </StyledAccordionGroup>

    </AbsoluteWrapper>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
  limitDuration: PropTypes.number,
  currentTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
  limitDuration: 0,
};

export default AbsoluteTimeRangeSelector;
