// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import { useEffect, useMemo, useState } from 'react';
import { useFormikContext, useField } from 'formik';
import moment from 'moment';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import { availableTimeRangeTypes } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm.js';
import DateTime from 'logic/datetimes/DateTime';
import { type ThemeInterface } from 'theme';

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
    max-width: 50vw;  
  }
  
  @media (min-width: ${theme.breakpoints.min.lg}) {
    max-width: 35vw;  
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

const timeRangeTypeTabs = (config, activeKey, originalRangeValue, limitDuration, setDisableApply) => availableTimeRangeTypes.map<RangeType>(({ type, name }) => {
  const RangeComponent = timeRangeTypes?.[type] || DisabledTimeRangeSelector;

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeKey && (
        <RangeComponent config={config}
                        disabled={false}
                        originalTimeRange={originalRangeValue}
                        limitDuration={limitDuration}
                        setDisableApply={setDisableApply} />
      )}
    </Tab>
  );
});

const TimeRangeDropdown = ({ config, noOverride, toggleDropdownShow }: Props) => {
  const formik = useFormikContext();
  const [originalTimerange, , originalTimerangeHelpers] = useField('timerange');
  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');
  const [disableApply, setDisableApply] = useState(false);
  const { value: nextRangeValue } = nextRangeProps;
  const { value: originalRangeValue } = originalTimerange;

  const [activeTab, setActiveTab] = useState(originalRangeValue?.type);

  useEffect(() => {
    if (!nextRangeValue) {
      nextRangeHelpers.setValue(originalRangeValue);
    }
  });

  const _setDisableApply = (isDisabled: boolean) => {
    if (disableApply !== isDisabled) {
      setDisableApply(isDisabled);
    }
  };

  const onSelect = (newType) => {
    nextRangeHelpers.setValue(migrateTimeRangeToNewType(nextRangeValue.type, newType));
    setActiveTab(newType);
  };

  const handleCancel = () => {
    formik.resetForm({
      values: { timerange: originalRangeValue, tempTimeRange: null },
    });

    toggleDropdownShow();
  };

  const handleApply = () => {
    originalTimerangeHelpers.setValue(nextRangeValue);
    formik.unregisterField('tempTimeRange');
    toggleDropdownShow();
  };

  const activeKey = activeTab || 'disabled';

  const limitDuration = useMemo(() => moment.duration(config.query_time_range_limit).asSeconds(), [config.query_time_range_limit]);

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
          <TimeRangeLivePreview timerange={nextRangeProps.value || originalTimerange.value} />

          <StyledTabs id="dateTimeTypes"
                      defaultActiveKey={availableTimeRangeTypes[0].type}
                      activeKey={activeKey}
                      onSelect={onSelect}
                      animation={false}>
            {noOverride && (
              <Tab title="No Override"
                   key="time-range-type-selector-disabled"
                   eventKey="disabled">
                <p>No Override to Date.</p>
              </Tab>
            )}
            {timeRangeTypeTabs(config, activeKey, originalRangeValue, limitDuration, _setDisableApply)}
          </StyledTabs>
        </Col>
      </Row>

      <Row className="row-sm">
        <Col md={6}>
          <Timezone>All timezones using: <b>{DateTime.getUserTimezone()}</b></Timezone>
        </Col>
        <Col md={6}>
          <div className="pull-right">
            <Button bsStyle="link" onClick={handleCancel}>Cancel</Button>
            <Button bsStyle="success" onClick={handleApply} disabled={disableApply}>Apply</Button>
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
