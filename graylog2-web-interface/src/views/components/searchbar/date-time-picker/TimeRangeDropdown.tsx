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
import styled, { css } from 'styled-components';
import { useFormikContext } from 'formik';
import moment from 'moment';
import Mousetrap from 'mousetrap';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import { availableTimeRangeTypes, FormikValues } from 'views/Constants';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm';
import DateTime from 'logic/datetimes/DateTime';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';
import TabKeywordTimeRange from './TabKeywordTimeRange';
import TabRelativeTimeRange from './TabRelativeTimeRange';
import TabDisabledTimeRange from './TabDisabledTimeRange';
import TimeRangeLivePreview from './TimeRangeLivePreview';
import { DateTimeContext } from './DateTimeProvider';

const timeRangeTypes = {
  absolute: TabAbsoluteTimeRange,
  relative: TabRelativeTimeRange,
  keyword: TabKeywordTimeRange,
};

type Props = {
  noOverride?: boolean,
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
};

const timeRangeTypeTabs = ({ activeTab, originalTimeRange, limitDuration, currentTimeRange }) => availableTimeRangeTypes.map(({ type, name }) => {
  const TimeRangeComponent = timeRangeTypes[type];

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeTab && (
        <TimeRangeComponent disabled={false}
                            originalTimeRange={originalTimeRange || DEFAULT_RANGES[type]}
                            limitDuration={limitDuration}
                            currentTimeRange={currentTimeRange || DEFAULT_RANGES[type]} />
      )}
    </Tab>
  );
});

const TimeRangeDropdown = ({ noOverride, toggleDropdownShow }: Props) => {
  const { limitDuration } = useContext(DateTimeContext);
  const { initialValues, isValid, setFieldValue, validateForm, values } = useFormikContext<FormikValues>();
  const originalTimeRange = initialValues?.timerange;
  const initialTimeRange = values?.timerange;
  const currentTimeRange = values?.nextTimeRange;

  const [activeTab, setActiveTab] = useState(currentTimeRange?.type);

  const handleNoOverride = () => {
    setFieldValue('timerange', {});
    setFieldValue('nextTimeRange', {});

    toggleDropdownShow();
  };

  const handleActiveTab = (nextTab) => {
    if (currentTimeRange?.type) {
      setFieldValue('nextTimeRange', migrateTimeRangeToNewType(currentTimeRange, nextTab), false);
    } else {
      setFieldValue('nextTimeRange', DEFAULT_RANGES[nextTab], false);
    }

    validateForm();
    setActiveTab(nextTab);
  };

  const handleCancel = useCallback(() => {
    setFieldValue('nextTimeRange', initialTimeRange);

    toggleDropdownShow();
  }, [initialTimeRange, setFieldValue, toggleDropdownShow]);

  const handleApply = useCallback(() => {
    setFieldValue('timerange', currentTimeRange);

    toggleDropdownShow();
  }, [currentTimeRange, setFieldValue, toggleDropdownShow]);

  const handleEnterKeyPress = useCallback(() => {
    if (isValid) {
      handleApply();
    }
  }, [isValid, handleApply]);

  const handleEscKeyPress = useCallback(() => {
    handleCancel();
  }, [handleCancel]);

  useEffect(() => {
    Mousetrap.bind('enter', handleEnterKeyPress);
    Mousetrap.bind('esc', handleEscKeyPress);

    return () => {
      Mousetrap.reset();
    };
  }, [handleEnterKeyPress, handleEscKeyPress]);

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
      <Row>
        <Col md={12}>
          <TimeRangeLivePreview timerange={currentTimeRange} />

          <StyledTabs id="dateTimeTypes"
                      defaultActiveKey={availableTimeRangeTypes[0].type}
                      activeKey={activeTab ?? -1}
                      onSelect={handleActiveTab}
                      animation={false}>
            {timeRangeTypeTabs({
              activeTab,
              originalTimeRange,
              limitDuration,
              currentTimeRange,
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
            <Button bsStyle="success" onClick={handleApply} disabled={!isValid}>Apply</Button>
          </div>
        </Col>
      </Row>
    </StyledPopover>
  );
};

TimeRangeDropdown.defaultProps = {
  noOverride: false,
};

export default TimeRangeDropdown;
