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
import { useContext, useCallback, useState } from 'react';
import { Form, Formik } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon, KeyCapture } from 'components/common';
import { availableTimeRangeTypes, RELATIVE_ALL_TIME } from 'views/Constants';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm';
import DateTime from 'logic/datetimes/DateTime';
import type { RelativeTimeRangeWithEnd, AbsoluteTimeRange, KeywordTimeRange, NoTimeRangeOverride, TimeRange } from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTypeRelativeWithEnd, isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import TimeRangeLivePreview from './TimeRangeLivePreview';
import { DateTimeContext } from './DateTimeProvider';

export type TimeRangeDropDownFormValues = {
  nextTimeRange: RelativeTimeRangeWithEnd | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride,
};

const timeRangeTypes = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

const allTimeRangeTypes = Object.keys(timeRangeTypes) as Array<TimeRangeType>;

type Props = {
  noOverride?: boolean,
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride,
  setCurrentTimeRange: (nextTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
  validTypes?: Array<TimeRangeType>,
};

const StyledPopover = styled(Popover)(({ theme }) => css`
  min-width: 100%;
  
  @media (min-width: ${theme.breakpoints.min.md}) {
    min-width: 750px;
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
  line-height: 34px;
`);

const PopoverTitle = styled.span`
  display: flex;
  justify-content: space-between;
  align-items: center;
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

const CancelButton = styled(Button)`
  margin-right: 6px;
`;

const DEFAULT_RANGES = {
  absolute: {
    type: 'absolute',
    from: DateTime.now().subtract(300, 'seconds').format(DateTime.Formats.TIMESTAMP),
    to: DateTime.now().format(DateTime.Formats.TIMESTAMP),
  },
  relative: {
    type: 'relative',
    from: 300,
  },
  keyword: {
    type: 'keyword',
    keyword: 'Last five minutes',
  },
  disabled: undefined,
};

export type TimeRangeType = keyof typeof timeRangeTypes;

type TimeRangeTabsArguments = {
  activeTab: TimeRangeType,
  limitDuration: number,
  setValidatingKeyword: (status: boolean) => void,
  tabs: Array<TimeRangeType>,
}

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

const exceedsDuration = (timerange, limitDuration) => {
  if (limitDuration === 0) {
    return false;
  }

  switch (timerange?.type) {
    case 'absolute':
    case 'keyword': { // eslint-disable-line no-fallthrough, padding-line-between-statements
      const durationFrom = timerange.from;
      const durationLimit = DateTime.now().subtract(Number(limitDuration), 'seconds').format(DateTime.Formats.TIMESTAMP);

      return moment(durationFrom).isBefore(durationLimit);
    }

    default:
      return false;
  }
};

export const dateTimeValidate = (nextTimeRange, limitDuration) => {
  const errors: { nextTimeRange?: {
    from?: string,
    to?: string,
    range?: string,
    keyword?: string,
  } } = {};

  const invalidDateFormatError = 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].';
  const rangeLimitError = 'Range is outside limit duration.';
  const dateLimitError = 'Date is outside limit duration.';
  const timeRangeError = 'The "Until" date must come after the "From" date.';

  if (nextTimeRange?.type === 'absolute') {
    if (!DateTime.isValidDateString(nextTimeRange.from)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: invalidDateFormatError };
    }

    if (!DateTime.isValidDateString(nextTimeRange.to)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: invalidDateFormatError };
    }

    if (nextTimeRange.from >= nextTimeRange.to) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: timeRangeError };
    }

    if (exceedsDuration(nextTimeRange, limitDuration)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: dateLimitError };
    }
  }

  if (nextTimeRange?.type === 'relative') {
    if (limitDuration > 0) {
      if (nextTimeRange.from > limitDuration || !nextTimeRange.from) {
        errors.nextTimeRange = { ...errors.nextTimeRange, from: rangeLimitError };
      }

      if (nextTimeRange.to > limitDuration) {
        errors.nextTimeRange = { ...errors.nextTimeRange, to: rangeLimitError };
      }
    }

    if (nextTimeRange.from && nextTimeRange.from <= nextTimeRange.to) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: timeRangeError };
    }
  }

  if (nextTimeRange?.type === 'keyword') {
    if (exceedsDuration(nextTimeRange, limitDuration)) {
      errors.nextTimeRange = { keyword: rangeLimitError };
    }
  }

  return errors;
};

const onInitializingNextTimeRange = (currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => {
  if (isTypeRelativeWithStartOnly(currentTimeRange)) {
    return {
      type: currentTimeRange.type,
      from: currentTimeRange.range,
    };
  }

  return currentTimeRange;
};

const onSettingCurrentTimeRange = (nextTimeRange: TimeRangeDropDownFormValues['nextTimeRange']) => {
  if (isTypeRelativeWithEnd(nextTimeRange) && nextTimeRange.from === RELATIVE_ALL_TIME) {
    return {
      type: nextTimeRange.type,
      range: nextTimeRange.from,
    };
  }

  return (nextTimeRange as SearchBarFormValues['timerange']);
};

const TimeRangeDropdown = ({ noOverride, toggleDropdownShow, currentTimeRange, setCurrentTimeRange, validTypes = allTimeRangeTypes }: Props) => {
  const { limitDuration } = useContext(DateTimeContext);
  const [validatingKeyword, setValidatingKeyword] = useState(false);
  const [activeTab, setActiveTab] = useState('type' in currentTimeRange ? currentTimeRange.type : undefined);

  const handleNoOverride = () => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  };

  const handleCancel = useCallback(() => {
    toggleDropdownShow();
  }, [toggleDropdownShow]);

  const handleSubmit = ({ nextTimeRange }) => {
    setCurrentTimeRange(onSettingCurrentTimeRange(nextTimeRange));
    toggleDropdownShow();
  };

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

  return (
    <StyledPopover id="timerange-type"
                   data-testid="timerange-type"
                   placement="bottom"
                   positionTop={36}
                   title={title}
                   arrowOffsetLeft={34}>
      <Formik initialValues={{ nextTimeRange: onInitializingNextTimeRange(currentTimeRange) }}
              validate={({ nextTimeRange }) => dateTimeValidate(nextTimeRange, limitDuration)}
              onSubmit={handleSubmit}
              validateOnMount>
        {(({ values: { nextTimeRange }, isValid, setFieldValue, submitForm }) => {
          const handleActiveTab = (nextTab) => {
            if ('type' in nextTimeRange) {
              setFieldValue('nextTimeRange', migrateTimeRangeToNewType(nextTimeRange as TimeRange, nextTab));
            } else {
              setFieldValue('nextTimeRange', DEFAULT_RANGES[nextTab]);
            }

            setActiveTab(nextTab);
          };

          return (
            <KeyCapture shortcuts={{ enter: submitForm, esc: handleCancel }}>
              <Form>
                <Row>
                  <Col md={12}>
                    <TimeRangeLivePreview timerange={nextTimeRange} />

                    <StyledTabs id="dateTimeTypes"
                                defaultActiveKey={availableTimeRangeTypes[0].type}
                                activeKey={activeTab ?? -1}
                                onSelect={handleActiveTab}
                                animation={false}>
                      {timeRangeTypeTabs({
                        activeTab,
                        limitDuration,
                        setValidatingKeyword,
                        tabs: validTypes,
                      })}

                      {!activeTab && (<TabDisabledTimeRange />)}

                    </StyledTabs>
                  </Col>
                </Row>

                <Row className="row-sm">
                  <Col md={6}>
                    <Timezone>All timezones using: <b>{DateTime.getUserTimezone()}</b></Timezone>
                  </Col>
                  <Col md={6}>
                    <div className="pull-right">
                      {noOverride && (
                        <Button bsStyle="link" onClick={handleNoOverride}>No Override</Button>
                      )}
                      <CancelButton bsStyle="default" onClick={handleCancel}>Cancel</CancelButton>
                      <Button bsStyle="success" disabled={!isValid || validatingKeyword} type="submit">Apply</Button>
                    </div>
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
