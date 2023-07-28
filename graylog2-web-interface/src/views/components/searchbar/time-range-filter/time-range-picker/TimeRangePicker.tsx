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
import { useCallback, useContext, useMemo, useState } from 'react';
import { Form, Formik } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Button, Col, Row, Popover } from 'components/bootstrap';
import { Icon, KeyCapture, ModalSubmit } from 'components/common';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  NoTimeRangeOverride,
  TimeRange,
} from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTypeKeyword, isTypeRelative } from 'views/typeGuards/timeRange';
import { normalizeIfAllMessagesRange } from 'views/logic/queries/NormalizeTimeRange';
import validateTimeRange from 'views/components/TimeRangeValidation';
import type { DateTime } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';

import type { RelativeTimeRangeClassified } from './types';
import {
  classifyRelativeTimeRange,
  normalizeIfClassifiedRelativeTimeRange,
} from './RelativeTimeRangeClassifiedHelper';
import TimeRangeTabs, { timeRangePickerTabs } from './TimeRangePickerTabs';
import TimeRangePresetRow from './TimeRangePresetRow';

export type TimeRangePickerFormValues = {
  nextTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride,
};

export type SupportedTimeRangeType = keyof typeof timeRangePickerTabs;

export const allTimeRangeTypes = Object.keys(timeRangePickerTabs) as Array<SupportedTimeRangeType>;

const StyledPopover = styled(Popover)(({ theme }) => css`
  min-width: 750px;
  background-color: ${theme.colors.variant.lightest.default};

  .popover-title {
    border: none;
  }
`);

const Timezone = styled.p(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  padding-left: 3px;
  margin: 0;
  min-height: 34px;
  display: flex;
  align-items: center;
`);

const PopoverTitle = styled.span`
  display: flex;
  justify-content: space-between;
  align-items: center;

  > span {
    font-weight: 600;
  }
`;

const LimitLabel = styled.span(({ theme }) => css`
  > svg {
    margin-right: 3px;
    color: ${theme.colors.variant.dark.warning};
  }

  > span {
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.variant.darkest.warning};
  }
`);

const dateTimeValidate = (nextTimeRange, limitDuration, formatTime: (dateTime: DateTime, format: string) => string) => {
  const timeRange = normalizeIfClassifiedRelativeTimeRange(nextTimeRange);
  const timeRangeErrors = validateTimeRange(timeRange, limitDuration, formatTime);

  return Object.keys(timeRangeErrors).length !== 0
    ? { nextTimeRange: timeRangeErrors }
    : {};
};

const onInitializingNextTimeRange = (currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => {
  if (isTypeRelative(currentTimeRange)) {
    return classifyRelativeTimeRange(currentTimeRange);
  }

  return currentTimeRange;
};

const normalizeIfKeywordTimeRange = (timeRange: TimeRange | NoTimeRangeOverride) => {
  if (isTypeKeyword(timeRange)) {
    return {
      type: timeRange.type,
      timezone: timeRange.timezone,
      keyword: timeRange.keyword,
    };
  }

  return timeRange;
};

type Props = {
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride,
  limitDuration: number,
  noOverride?: boolean,
  position: 'bottom' | 'right',
  setCurrentTimeRange: (nextTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
  validTypes?: Array<SupportedTimeRangeType>,
};

const TimeRangePicker = ({
  noOverride,
  toggleDropdownShow,
  currentTimeRange,
  setCurrentTimeRange,
  validTypes = allTimeRangeTypes,
  position,
  limitDuration: configLimitDuration,
}: Props) => {
  const { ignoreLimitDurationInTimeRangeDropdown } = useContext(TimeRangeInputSettingsContext);
  const limitDuration = useMemo(() => (ignoreLimitDurationInTimeRangeDropdown ? 0 : configLimitDuration), [configLimitDuration, ignoreLimitDurationInTimeRangeDropdown]);
  const { formatTime, userTimezone } = useUserDateTime();
  const [validatingKeyword, setValidatingKeyword] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const positionIsBottom = position === 'bottom';

  const handleNoOverride = useCallback(() => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  }, [setCurrentTimeRange, toggleDropdownShow]);

  const handleCancel = useCallback(() => {
    toggleDropdownShow();

    sendTelemetry('click', {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'search-time-range-cancel-button',
    });
  }, [sendTelemetry, toggleDropdownShow]);

  const handleSubmit = useCallback(({ nextTimeRange }: {
    nextTimeRange: TimeRangePickerFormValues['nextTimeRange']
  }) => {
    const normalizedTimeRange = normalizeIfKeywordTimeRange(
      normalizeIfAllMessagesRange(
        normalizeIfClassifiedRelativeTimeRange(nextTimeRange),
      ),
    );

    setCurrentTimeRange(normalizedTimeRange);

    toggleDropdownShow();

    sendTelemetry('click', {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'search-time-range-confirm-button',
    });
  }, [sendTelemetry, setCurrentTimeRange, toggleDropdownShow]);

  const title = (
    <PopoverTitle>
      <span>Search Time Range</span>
      {limitDuration > 0 && (
        <LimitLabel>
          <Icon name="exclamation-triangle" />
          <span>Admin has limited searching to {moment.duration(-limitDuration, 'seconds').humanize(true)}</span>
        </LimitLabel>
      )}
    </PopoverTitle>
  );

  const _validateTimeRange = useCallback(({ nextTimeRange }) => dateTimeValidate(nextTimeRange, limitDuration, formatTime), [formatTime, limitDuration]);
  const initialTimeRange = useMemo(() => ({ nextTimeRange: onInitializingNextTimeRange(currentTimeRange) }), [currentTimeRange]);

  return (
    <StyledPopover id="timerange-type"
                   data-testid="timerange-type"
                   placement={position}
                   positionTop={positionIsBottom ? 36 : -10}
                   positionLeft={positionIsBottom ? -15 : 45}
                   arrowOffsetTop={positionIsBottom ? undefined : 25}
                   arrowOffsetLeft={positionIsBottom ? 34 : -11}
                   title={title}>
      <Formik<TimeRangePickerFormValues> initialValues={initialTimeRange}
                                         validate={_validateTimeRange}
                                         onSubmit={handleSubmit}
                                         validateOnMount>
        {(({ isValid, submitForm }) => (
          <KeyCapture shortcuts={{ enter: submitForm, esc: handleCancel }}>
            <Form>
              <Row>
                <Col md={12}>
                  <TimeRangePresetRow />
                  <TimeRangeTabs limitDuration={limitDuration}
                                 validTypes={validTypes}
                                 setValidatingKeyword={setValidatingKeyword} />
                </Col>
              </Row>

              <Row className="row-sm">
                <Col md={6}>
                  <Timezone>All timezones using: <b>{userTimezone}</b></Timezone>
                </Col>
                <Col md={6}>
                  <ModalSubmit leftCol={noOverride && <Button bsStyle="link" onClick={handleNoOverride}>No Override</Button>}
                               onCancel={handleCancel}
                               disabledSubmit={!isValid || validatingKeyword}
                               submitButtonText="Update time range" />
                </Col>
              </Row>
            </Form>
          </KeyCapture>
        ))}
      </Formik>
    </StyledPopover>
  );
};

TimeRangePicker.defaultProps = {
  noOverride: false,
  validTypes: allTimeRangeTypes,
};

export default TimeRangePicker;
