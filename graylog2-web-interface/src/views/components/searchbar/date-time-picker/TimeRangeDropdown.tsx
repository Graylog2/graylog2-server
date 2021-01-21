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
import { useContext, useCallback, useEffect, useState } from 'react';
import { Form, Formik } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';
import Mousetrap from 'mousetrap';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import { availableTimeRangeTypes } from 'views/Constants';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm';
import DateTime from 'logic/datetimes/DateTime';
import type { NoTimeRangeOverride, TimeRange } from 'views/logic/queries/Query';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import TimeRangeLivePreview from './TimeRangeLivePreview';
import { DateTimeContext } from './DateTimeProvider';

export type TimeRangeDropDownFormValues = {
  nextTimeRange: TimeRange | NoTimeRangeOverride,
};

const timeRangeTypes = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

type Props = {
  noOverride?: boolean,
  currentTimeRange: TimeRange | NoTimeRangeOverride,
  setCurrentTimeRange: (nextTimeRange: TimeRange | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
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
    range: 300,
  },
  keyword: {
    type: 'keyword',
    keyword: 'Last five minutes',
  },
  disabled: undefined,
};

const timeRangeTypeTabs = ({ activeTab, limitDuration, setValidatingKeyword }) => availableTimeRangeTypes.map(({ type, name }) => {
  const TimeRangeTypeTabs = timeRangeTypes[type];

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeTab && (
        <TimeRangeTypeTabs disabled={false}
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

    case 'relative':
      return timerange.range > limitDuration;

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

  if (nextTimeRange?.type === 'absolute') {
    if (!DateTime.isValidDateString(nextTimeRange.from)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (!DateTime.isValidDateString(nextTimeRange.to)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (nextTimeRange.from > nextTimeRange.to) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: 'The "Until" date must come after the "From" date.' };
    }

    if (exceedsDuration(nextTimeRange, limitDuration)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Date is outside limit duration.' };
    }
  }

  if (nextTimeRange?.type === 'relative') {
    if (exceedsDuration(nextTimeRange, limitDuration)) {
      errors.nextTimeRange = { range: 'Range is outside limit duration.' };
    }
  }

  if (nextTimeRange?.type === 'keyword') {
    if (exceedsDuration(nextTimeRange, limitDuration)) {
      errors.nextTimeRange = { keyword: 'Date is outside limit duration.' };
    }
  }

  return errors;
};

const TimeRangeDropdown = ({ noOverride, toggleDropdownShow, currentTimeRange, setCurrentTimeRange }: Props) => {
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
    setCurrentTimeRange(nextTimeRange);
    toggleDropdownShow();
  };

  const handleEscKeyPress = useCallback(() => {
    handleCancel();
  }, [handleCancel]);

  useEffect(() => {
    Mousetrap.bind('esc', handleEscKeyPress);

    return () => {
      Mousetrap.reset();
    };
  }, [handleEscKeyPress]);

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
                   placement="bottom"
                   positionTop={36}
                   title={title}
                   arrowOffsetLeft={34}>
      <Formik initialValues={{ nextTimeRange: currentTimeRange }}
              validate={({ nextTimeRange }) => dateTimeValidate(nextTimeRange, limitDuration)}
              onSubmit={handleSubmit}
              validateOnMount>
        {(({ values: { nextTimeRange }, isValid, setFieldValue }) => {
          const handleActiveTab = (nextTab) => {
            if ('type' in nextTimeRange) {
              setFieldValue('nextTimeRange', migrateTimeRangeToNewType(nextTimeRange as TimeRange, nextTab), true);
            } else {
              setFieldValue('nextTimeRange', DEFAULT_RANGES[nextTab], false);
            }

            setActiveTab(nextTab);
          };

          return (
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
          );
        })}
      </Formik>
    </StyledPopover>
  );
};

TimeRangeDropdown.defaultProps = {
  noOverride: false,
};

export default TimeRangeDropdown;
