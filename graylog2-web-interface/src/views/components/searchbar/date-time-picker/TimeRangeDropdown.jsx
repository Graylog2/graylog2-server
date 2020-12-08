// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import { useEffect, useMemo, useState } from 'react';
import { useFormikContext, useField } from 'formik';
import moment from 'moment';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import { availableTimeRangeTypes, DEFAULT_RANGES } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm';
import DateTime from 'logic/datetimes/DateTime';
import { type ThemeInterface } from 'theme';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from './KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from './RelativeTimeRangeSelector';
import TimeRangeLivePreview from './TimeRangeLivePreview';

const timeRangeTypes = {
  absolute: AbsoluteTimeRangeSelector,
  relative: RelativeTimeRangeSelector,
  keyword: KeywordTimeRangeSelector,
};

type Props = {
  config: SearchesConfig,
  noOverride?: boolean,
  toggleDropdownShow: (void) => void,
};

type RangeType = React.Element<Tab>;

const StyledPopover: StyledComponent<{}, ThemeInterface, typeof Popover> = styled(Popover)(({ theme }) => css`
  max-width: 100%; 
  min-width: 745px;
  
  @media (min-width: ${theme.breakpoints.min.md}) {
    max-width: 70vw;  
  }
  
  @media (min-width: ${theme.breakpoints.min.lg}) {
    max-width: 45vw;  
  }
`);

const StyledTabs: StyledComponent<{}, void, typeof Tabs> = styled(Tabs)`
  margin-top: 1px;
`;

const Timezone: StyledComponent<{}, ThemeInterface, HTMLParagraphElement> = styled.p(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  padding-left: 3px;
  margin: 0;
  line-height: 34px;
`);

const PopoverTitle: StyledComponent<{}, void, HTMLSpanElement> = styled.span`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const LimitLabel: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  > svg {
    margin-right: 3px;
    color: ${theme.colors.variant.dark.warning};
  }
  
  > span {
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.variant.darkest.warning};
  }
`);

const CancelButton: StyledComponent<{}, void, typeof Button> = styled(Button)`
  margin-right: 6px;
`;

const tabNames = (timerange) => {
  if (!timerange || Object.is(timerange, {})) {
    return '';
  }

  if (timerange?.range >= 0) {
    return 'relative';
  }

  if (timerange?.keyword) {
    return 'keyword';
  }

  return 'absolute';
};

const timeRangeTypeTabs = (activeKey, nextRangeValue, limitDuration, currentTimerange) => availableTimeRangeTypes.map<RangeType>(({ type, name }) => {
  const RangeComponent = timeRangeTypes?.[type];

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeKey && (
        <RangeComponent disabled={false}
                        originalTimeRange={nextRangeValue || DEFAULT_RANGES[type]}
                        limitDuration={limitDuration}
                        currentTimerange={currentTimerange} />
      )}
    </Tab>
  );
});

const TimeRangeDropdown = ({ config, noOverride, toggleDropdownShow }: Props) => {
  const formik = useFormikContext();
  const originalTimerange = useField('timerange')[0];
  const nextRangeProps = useField('tempTimeRange')[0];

  const originalRangeValue = useMemo(() => originalTimerange?.value, [originalTimerange]);
  const nextRangeValue = useMemo(() => nextRangeProps?.value || originalRangeValue, [nextRangeProps, originalRangeValue]);
  const limitDuration = useMemo(() => moment.duration(config.query_time_range_limit).asSeconds(), [config.query_time_range_limit]);
  const currentTimerange = useMemo(() => nextRangeValue || originalRangeValue, [nextRangeValue, originalRangeValue]);

  const [activeTab, setActiveTab] = useState(tabNames(originalRangeValue));

  useEffect(() => {
    if (nextRangeValue?.type) {
      formik.setFieldValue('tempTimeRange', migrateTimeRangeToNewType(nextRangeValue.type, activeTab));
    } else {
      formik.setFieldValue('tempTimeRange', DEFAULT_RANGES[activeTab]);
    }
  }, [activeTab, formik, nextRangeValue.type]);

  const handleNoOverride = () => {
    formik.setFieldValue('timerange', {});
    formik.setFieldValue('tempTimeRange', {});

    toggleDropdownShow();
  };

  const handleCancel = () => {
    formik.setFieldValue('timerange', originalRangeValue);
    formik.setFieldValue('tempTimeRange', originalRangeValue);

    toggleDropdownShow();
  };

  const handleApply = () => {
    formik.setFieldValue('timerange', nextRangeValue);
    formik.unregisterField('tempTimeRange');
    toggleDropdownShow();
  };

  const title = (
    <PopoverTitle>
      <span>Search Timerange</span>
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
          <TimeRangeLivePreview timerange={currentTimerange} />

          <StyledTabs id="dateTimeTypes"
                      defaultActiveKey={availableTimeRangeTypes[0].type}
                      activeKey={activeTab}
                      onSelect={setActiveTab}
                      animation={false}>
            {timeRangeTypeTabs(activeTab, nextRangeValue, limitDuration, currentTimerange)}
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
            <Button bsStyle="success" onClick={handleApply} disabled={!formik.isValid}>Apply</Button>
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
