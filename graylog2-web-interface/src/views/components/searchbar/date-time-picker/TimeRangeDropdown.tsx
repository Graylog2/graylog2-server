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
import { Form, Formik } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/bootstrap';
import { Icon, KeyCapture, ModalSubmit } from 'components/common';
import { availableTimeRangeTypes } from 'views/Constants';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  NoTimeRangeOverride,
  TimeRange,
  RelativeTimeRange,
} from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTypeRelative } from 'views/typeGuards/timeRange';
import { normalizeIfAllMessagesRange } from 'views/logic/queries/NormalizeTimeRange';
import type { RelativeTimeRangeClassified } from 'views/components/searchbar/date-time-picker/types';
import validateTimeRange from 'views/components/TimeRangeValidation';
import type { DateTimeFormats, DateTime } from 'util/DateTime';
import { toDateObject } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

import migrateTimeRangeToNewType from './migrateTimeRangeToNewType';
import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import TimeRangeLivePreview from './TimeRangeLivePreview';
import { classifyRelativeTimeRange, normalizeIfClassifiedRelativeTimeRange, RELATIVE_CLASSIFIED_ALL_TIME_RANGE } from './RelativeTimeRangeClassifiedHelper';

export type TimeRangeDropDownFormValues = {
  nextTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride,
};

export type TimeRangeDropdownProps = {
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride,
  limitDuration: number,
  noOverride?: boolean,
  position: 'bottom'|'right',
  setCurrentTimeRange: (nextTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
  validTypes?: Array<TimeRangeType>,
};

export type TimeRangeType = keyof typeof timeRangeTypes;

type TimeRangeTabsArguments = {
  activeTab: TimeRangeType,
  limitDuration: number,
  setValidatingKeyword: (status: boolean) => void,
  tabs: Array<TimeRangeType>,
}

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

const timeRangeTypes = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

const allTimeRangeTypes = Object.keys(timeRangeTypes) as Array<TimeRangeType>;

const StyledPopover = styled(Popover)(({ theme }) => css`
  min-width: 750px;
  background-color: ${theme.colors.variant.lightest.default};

  .popover-title {
    border: none;
  }
`);

const StyledTabs = styled(Tabs)`
  margin-top: 1px;
  margin-bottom: 9px;
`;

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

const timeRangeTypeTabs = ({ activeTab, limitDuration, setValidatingKeyword, tabs }: TimeRangeTabsArguments) => availableTimeRangeTypes
  .filter(({ type }) => tabs.includes(type))
  .map(({ type, name }) => {
    const TimeRangeTypeTab = timeRangeTypes[type];

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

type TimeRangeTabsProps = {
  handleActiveTab: (nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => void,
  currentTimeRange: NoTimeRangeOverride | TimeRange,
  limitDuration: number,
  validTypes: Array<'absolute' | 'relative' | 'keyword'>,
  setValidatingKeyword: (validating: boolean) => void,
};

const TimeRangeTabs = ({ handleActiveTab, currentTimeRange, limitDuration, validTypes, setValidatingKeyword }: TimeRangeTabsProps) => {
  const [activeTab, setActiveTab] = useState('type' in currentTimeRange ? currentTimeRange.type : undefined);

  const onSelect = useCallback((nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => {
    handleActiveTab(nextTab);
    setActiveTab(nextTab);
  }, [handleActiveTab]);

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

const TimeRangeDropdown = ({
  noOverride,
  toggleDropdownShow,
  currentTimeRange,
  setCurrentTimeRange,
  validTypes = allTimeRangeTypes,
  position,
  limitDuration,
}: TimeRangeDropdownProps) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const [validatingKeyword, setValidatingKeyword] = useState(false);

  const positionIsBottom = position === 'bottom';
  const defaultRanges = useMemo(() => createDefaultRanges(formatTime), [formatTime]);

  const handleNoOverride = useCallback(() => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  }, [setCurrentTimeRange, toggleDropdownShow]);

  const handleCancel = useCallback(() => {
    toggleDropdownShow();
  }, [toggleDropdownShow]);

  const handleSubmit = useCallback(({ nextTimeRange }: { nextTimeRange: TimeRangeDropDownFormValues['nextTimeRange'] }) => {
    setCurrentTimeRange(normalizeIfAllMessagesRange(normalizeIfClassifiedRelativeTimeRange(nextTimeRange)));

    toggleDropdownShow();
  }, [setCurrentTimeRange, toggleDropdownShow]);

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
  const initialTimeRange = { nextTimeRange: onInitializingNextTimeRange(currentTimeRange) };

  return (
    <StyledPopover id="timerange-type"
                   data-testid="timerange-type"
                   placement={position}
                   positionTop={positionIsBottom ? 36 : -10}
                   positionLeft={positionIsBottom ? -15 : 45}
                   arrowOffsetTop={positionIsBottom ? undefined : 25}
                   arrowOffsetLeft={positionIsBottom ? 34 : -11}
                   title={title}>
      <Formik<TimeRangeDropDownFormValues> initialValues={initialTimeRange}
                                           validate={_validateTimeRange}
                                           onSubmit={handleSubmit}
                                           validateOnMount>
        {(({ values: { nextTimeRange }, isValid, setFieldValue, submitForm }) => {
          const handleActiveTab = (nextTab: AbsoluteTimeRange['type'] | RelativeTimeRange['type'] | KeywordTimeRange['type']) => {
            if ('type' in nextTimeRange) {
              setFieldValue('nextTimeRange', migrateTimeRangeToNewType(nextTimeRange as TimeRange, nextTab, formatTime));
            } else {
              setFieldValue('nextTimeRange', defaultRanges[nextTab]);
            }
          };

          return (
            <KeyCapture shortcuts={{ enter: submitForm, esc: handleCancel }}>
              <Form>
                <Row>
                  <Col md={12}>
                    <TimeRangeLivePreview timerange={normalizeIfClassifiedRelativeTimeRange(nextTimeRange)} />

                    <TimeRangeTabs currentTimeRange={currentTimeRange}
                                   handleActiveTab={handleActiveTab}
                                   limitDuration={limitDuration}
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
          );
        })}
      </Formik>
    </StyledPopover>
  );
};

TimeRangeDropdown.defaultProps = {
  noOverride: false,
  validTypes: allTimeRangeTypes,
};

export default TimeRangeDropdown;
