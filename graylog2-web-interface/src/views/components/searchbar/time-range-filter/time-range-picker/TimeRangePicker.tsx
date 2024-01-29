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

import { Button, Col, Row } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import { Icon, KeyCapture, ModalSubmit } from 'components/common';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  NoTimeRangeOverride,
} from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTimeRange, isTypeRelative } from 'views/typeGuards/timeRange';
import {
  normalizeFromPickerForSearchBar,
} from 'views/logic/queries/NormalizeTimeRange';
import validateTimeRange from 'views/components/TimeRangeValidation';
import type { DateTime } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { RelativeTimeRangeClassified } from './types';
import {
  classifyRelativeTimeRange,
  normalizeIfClassifiedRelativeTimeRange,
} from './RelativeTimeRangeClassifiedHelper';
import TimeRangeTabs, { timeRangePickerTabs } from './TimeRangePickerTabs';
import TimeRangePresetRow from './TimeRangePresetRow';

export type TimeRangePickerFormValues = {
  timeRangeTabs: {
    relative?: RelativeTimeRangeClassified | undefined,
    absolute?: AbsoluteTimeRange | undefined,
    keyword?: KeywordTimeRange | undefined,
  }
  activeTab: 'relative' | 'absolute' | 'keyword' | undefined
};

export type TimeRangePickerTimeRange = TimeRangePickerFormValues['timeRangeTabs'][keyof TimeRangePickerFormValues['timeRangeTabs']];
export type SupportedTimeRangeType = keyof typeof timeRangePickerTabs;

export const allTimeRangeTypes = Object.keys(timeRangePickerTabs) as Array<SupportedTimeRangeType>;

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

const dateTimeValidate = (activeTabTimeRange: TimeRangePickerTimeRange, limitDuration, formatTime: (dateTime: DateTime, format: string) => string) => {
  if (!activeTabTimeRange) {
    return {};
  }

  const normalizedTimeRange = normalizeIfClassifiedRelativeTimeRange(activeTabTimeRange);
  const timeRangeErrors = validateTimeRange(normalizedTimeRange, limitDuration, formatTime);

  return Object.keys(timeRangeErrors).length !== 0
    ? { timeRangeTabs: { [activeTabTimeRange.type]: timeRangeErrors } }
    : {};
};

const initialFormValues = (currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => {
  if (isTimeRange(currentTimeRange)) {
    return ({
      timeRangeTabs: {
        [currentTimeRange.type]: isTypeRelative(currentTimeRange) ? classifyRelativeTimeRange(currentTimeRange) : currentTimeRange,
      },
      activeTab: currentTimeRange.type,
    });
  }

  return ({
    timeRangeTabs: {},
    activeTab: undefined,
  });
};

type Props = React.PropsWithChildren<{
  show: boolean,
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride,
  limitDuration: number,
  noOverride?: boolean,
  position: 'bottom' | 'right',
  setCurrentTimeRange: (timeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
  validTypes?: Array<SupportedTimeRangeType>,
  withinPortal?: boolean,
}>;

const TimeRangePicker = ({
  children,
  show,
  noOverride,
  toggleDropdownShow,
  currentTimeRange,
  setCurrentTimeRange,
  validTypes = allTimeRangeTypes,
  position,
  limitDuration: configLimitDuration,
  withinPortal,
}: Props) => {
  const { ignoreLimitDurationInTimeRangeDropdown } = useContext(TimeRangeInputSettingsContext);
  const limitDuration = useMemo(() => (ignoreLimitDurationInTimeRangeDropdown ? 0 : configLimitDuration), [configLimitDuration, ignoreLimitDurationInTimeRangeDropdown]);
  const { formatTime, userTimezone } = useUserDateTime();
  const [validatingKeyword, setValidatingKeyword] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();

  const handleNoOverride = useCallback(() => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  }, [setCurrentTimeRange, toggleDropdownShow]);

  const handleCancel = useCallback(() => {
    toggleDropdownShow();

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_CANCELED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'search-time-range-cancel-button',
    });
  }, [location.pathname, sendTelemetry, toggleDropdownShow]);

  const handleSubmit = useCallback(({ timeRangeTabs, activeTab }: TimeRangePickerFormValues) => {
    const normalizedTimeRange = normalizeFromPickerForSearchBar(timeRangeTabs[activeTab]);

    setCurrentTimeRange(normalizedTimeRange);

    toggleDropdownShow();

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_UPDATED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'search-time-range-confirm-button',
      event_details: {
        timerange: normalizedTimeRange,
      },
    });
  }, [location.pathname, sendTelemetry, setCurrentTimeRange, toggleDropdownShow]);

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

  const _validateTimeRange = useCallback(({
    timeRangeTabs,
    activeTab,
  }: TimeRangePickerFormValues) => dateTimeValidate(timeRangeTabs[activeTab], limitDuration, formatTime), [formatTime, limitDuration]);
  const initialValues = useMemo(() => initialFormValues(currentTimeRange), [currentTimeRange]);

  return (
    <Popover id="timerange-type"
             data-testid="timerange-type"
             opened={show}
             position={position}
             withinPortal={withinPortal}
             withArrow
             width={735}
             zIndex={1060}>
      <Popover.Target>
        {children}
      </Popover.Target>
      <Popover.Dropdown title={title}>
        <Formik<TimeRangePickerFormValues> initialValues={initialValues}
                                           validate={_validateTimeRange}
                                           onSubmit={handleSubmit}
                                           validateOnMount>
          {(({ isValid, submitForm }) => (
            <KeyCapture shortcuts={[
              { actionKey: 'submit-form', callback: submitForm, scope: 'general', options: { displayInOverview: false } },
              { actionKey: 'close-modal', callback: handleCancel, scope: 'general', options: { displayInOverview: false } },
            ]}>
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
      </Popover.Dropdown>
    </Popover>
  );
};

TimeRangePicker.defaultProps = {
  noOverride: false,
  validTypes: allTimeRangeTypes,
  withinPortal: true,
};

export default TimeRangePicker;
