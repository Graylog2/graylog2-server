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
import styled, { css } from 'styled-components';
import { useState } from 'react';
import { Form, Formik } from 'formik';
import moment from 'moment';

import type { TimeRange } from 'views/logic/queries/Query';
import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import { availableTimeRangeTypes, DEFAULT_RANGE_TYPE } from 'views/Constants';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm';
import DateTime from 'logic/datetimes/DateTime';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from './KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from './RelativeTimeRangeSelector';
import DisabledTimeRangeSelector from './DisabledTimeRangeSelector';
import TimeRangeLivePreview from './TimeRangeLivePreview';

const timeRangeTypes = {
  absolute: AbsoluteTimeRangeSelector,
  relative: RelativeTimeRangeSelector,
  keyword: KeywordTimeRangeSelector,
};

export type TimeRangeDropDownFormValues = {
  nextTimeRange?: TimeRange,
};

const DEFAULT_RANGES = {
  absolute: {
    type: 'absolute',
    from: moment().subtract(300, 'seconds').format(DateTime.Formats.TIMESTAMP),
    to: moment().format(DateTime.Formats.TIMESTAMP),
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
} as const;

type NoTimeRangeOverride = {};

type Props = {
  noOverride?: boolean,
  toggleDropdownShow: () => void,
  limitDuration: number,
  currentTimeRange: TimeRange | NoTimeRangeOverride,
  setCurrentTimeRange: (nextTimeRange: TimeRange | NoTimeRangeOverride) => void,
};

const StyledPopover = styled(Popover)(({ theme }) => css`
  max-width: 100%;
  min-width: 745px;
  
  @media (min-width: ${theme.breakpoints.min.md}) {
    max-width: 70vw;
  }
  
  @media (min-width: ${theme.breakpoints.min.lg}) {
    max-width: 45vw;
  }
`);

const StyledTabs = styled(Tabs)`
  margin-top: 1px;
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

const timeRangeTypeTabs = ({ activeTab, limitDuration }) => availableTimeRangeTypes.map(({ type, name }) => {
  const RangeComponent = timeRangeTypes?.[type] || DisabledTimeRangeSelector;

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeTab && (
        <RangeComponent disabled={false}
                        limitDuration={limitDuration} />
      )}
    </Tab>
  );
});

export const dateTimeValidate = ({ nextTimeRange }, limitDuration) => {
  const errors: {
      nextTimeRange?: {
        from?: string,
        to?: string,
        range?: string,
    }
  } = {};

  if (nextTimeRange?.type === 'absolute') {
    if (!DateTime.isValidDateString(nextTimeRange.from)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (!DateTime.isValidDateString(nextTimeRange.to)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (nextTimeRange.from > nextTimeRange.to) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Start date must be before end date' };
    }
  }

  if (nextTimeRange?.type === 'relative') {
    if (!(limitDuration === 0 || (nextTimeRange.range <= limitDuration && limitDuration !== 0))) {
      errors.nextTimeRange = { range: 'Range is outside limit duration.' };
    }
  }

  return errors;
};

const TimeRangeDropdown = ({ noOverride, toggleDropdownShow, setCurrentTimeRange, currentTimeRange, limitDuration }: Props) => {
  const [activeTab, setActiveTab] = useState('type' in currentTimeRange ? currentTimeRange.type : DEFAULT_RANGE_TYPE);

  const handleNoOverride = () => {
    setCurrentTimeRange({});
    toggleDropdownShow();
  };

  const handleCancel = () => {
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
                   placement="bottom"
                   positionTop={36}
                   title={title}
                   arrowOffsetLeft={34}>
      <Formik initialValues={{ nextTimeRange: 'type' in currentTimeRange ? currentTimeRange : DEFAULT_RANGES[activeTab] }}
              validate={(values) => dateTimeValidate(values, limitDuration)}
              onSubmit={({ nextTimeRange }) => {
                setCurrentTimeRange(nextTimeRange);
                toggleDropdownShow();
              }}>
        {(({ values: { nextTimeRange }, isValid, setFieldValue, validateForm }) => {
          const changeTab = (nextTimeRangeType) => {
            if (nextTimeRangeType !== nextTimeRange?.type) {
              if (nextTimeRange?.type) {
                setFieldValue('nextTimeRange', migrateTimeRangeToNewType(nextTimeRange, nextTimeRangeType), false);
              } else {
                setFieldValue('nextTimeRange', DEFAULT_RANGES[activeTab], false);
              }
            }

            setActiveTab(nextTimeRangeType);
            validateForm();
          };

          return (
            <Form>
              <Row>
                <Col md={12}>
                  <TimeRangeLivePreview timerange={nextTimeRange} />

                  <StyledTabs id="dateTimeTypes"
                              defaultActiveKey={availableTimeRangeTypes[0].type}
                              activeKey={activeTab}
                              onSelect={changeTab}
                              animation={false}>
                    {timeRangeTypeTabs({
                      activeTab,
                      limitDuration,
                    })}
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
                    <Button bsStyle="success" type="submit" disabled={!isValid}>Apply</Button>
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
