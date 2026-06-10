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
import { Formik } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Button } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import { Icon, KeyCapture, ModalSubmit, NestedForm } from 'components/common';
import type { NoTimeRangeOverride } from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTimeRange, isTypeRelative } from 'views/typeGuards/timeRange';
import { normalizeFromPickerForSearchBar } from 'views/logic/queries/NormalizeTimeRange';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { SupportedTimeRangeType, TimeRangePickerFormValues } from './types';
import { classifyRelativeTimeRange } from './RelativeTimeRangeClassifiedHelper';
import TimeRangePickerFormContent, { allTimeRangeTypes } from './TimeRangePickerFormContent';
import useTimeRangeValidation from './useTimeRangeValidation';

const PopoverTitle = styled.span`
  display: flex;
  justify-content: space-between;
  align-items: center;

  > span {
    font-weight: 600;
  }
`;

const LimitLabel = styled.span(
  ({ theme }) => css`
    > svg {
      margin-right: 3px;
      color: ${theme.colors.variant.dark.warning};
    }

    > span {
      font-size: ${theme.fonts.size.small};
      color: ${theme.colors.variant.darkest.warning};
    }
  `,
);

const initialFormValues = (currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => {
  if (isTimeRange(currentTimeRange)) {
    return {
      timeRangeTabs: {
        [currentTimeRange.type]: isTypeRelative(currentTimeRange)
          ? classifyRelativeTimeRange(currentTimeRange)
          : currentTimeRange,
      },
      activeTab: currentTimeRange.type,
    };
  }

  return {
    timeRangeTabs: {},
    activeTab: undefined,
  };
};

type Props = React.PropsWithChildren<{
  show: boolean;
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride;
  limitDuration: number;
  noOverride?: boolean;
  position: 'bottom' | 'bottom-start' | 'right';
  setCurrentTimeRange: (timeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void;
  toggleDropdownShow: () => void;
  validTypes?: Array<SupportedTimeRangeType>;
  withinPortal?: boolean;
}>;

const TimeRangePicker = ({
  children = undefined,
  show,
  noOverride = false,
  toggleDropdownShow,
  currentTimeRange,
  setCurrentTimeRange,
  validTypes = allTimeRangeTypes,
  position,
  limitDuration,
  withinPortal = true,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const validateTimeRange = useTimeRangeValidation(limitDuration);

  const handleNoOverride = useCallback(() => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  }, [setCurrentTimeRange, toggleDropdownShow]);

  const handleCancel = useCallback(() => {
    toggleDropdownShow();

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_CANCELED, {
      app_section: 'search-bar',
      app_action_value: 'search-time-range-cancel-button',
    });
  }, [sendTelemetry, toggleDropdownShow]);

  const onSubmit = useCallback(
    ({ timeRangeTabs, activeTab }: TimeRangePickerFormValues) => {
      const normalizedTimeRange = normalizeFromPickerForSearchBar(timeRangeTabs[activeTab]);

      setCurrentTimeRange(normalizedTimeRange);

      toggleDropdownShow();

      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_UPDATED, {
        app_section: 'search-bar',
        app_action_value: 'search-time-range-confirm-button',
        event_details: {
          timerange: normalizedTimeRange,
        },
      });
    },
    [sendTelemetry, setCurrentTimeRange, toggleDropdownShow],
  );

  const title = (
    <PopoverTitle>
      <span>Search Time Range</span>
      {limitDuration > 0 && (
        <LimitLabel>
          <Icon name="warning" />
          <span>Admin has limited searching to {moment.duration(-limitDuration, 'seconds').humanize(true)}</span>
        </LimitLabel>
      )}
    </PopoverTitle>
  );

  const initialValues = useMemo(() => initialFormValues(currentTimeRange), [currentTimeRange]);

  return (
    <Popover
      id="timerange-type"
      data-testid="timerange-type"
      opened={show}
      onChange={toggleDropdownShow}
      position={position}
      withinPortal={withinPortal}
      withArrow
      width={735}
      zIndex={1060}>
      <Popover.Target>{children}</Popover.Target>
      <Popover.Dropdown title={title}>
        <Formik<TimeRangePickerFormValues>
          initialValues={initialValues}
          validate={validateTimeRange}
          onSubmit={onSubmit}
          validateOnMount>
          {({ isValid, submitForm, isValidating }) => (
            <KeyCapture
              shortcuts={[
                {
                  actionKey: 'submit-form',
                  callback: submitForm,
                  scope: 'general',
                  options: { displayInOverview: false },
                },
                {
                  actionKey: 'close-modal',
                  callback: handleCancel,
                  scope: 'general',
                  options: { displayInOverview: false },
                },
              ]}>
              <NestedForm>
                <TimeRangePickerFormContent limitDuration={limitDuration} validTypes={validTypes}>
                  <ModalSubmit
                    leftCol={
                      noOverride && (
                        <Button bsStyle="link" onClick={handleNoOverride}>
                          No Override
                        </Button>
                      )
                    }
                    onCancel={handleCancel}
                    disabledSubmit={!isValid || isValidating}
                    submitButtonText="Update time range"
                  />
                </TimeRangePickerFormContent>
              </NestedForm>
            </KeyCapture>
          )}
        </Formik>
      </Popover.Dropdown>
    </Popover>
  );
};

export default TimeRangePicker;
