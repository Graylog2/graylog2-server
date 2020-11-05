// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import { useEffect, useState } from 'react';
import { useFormikContext, useField } from 'formik';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { availableTimeRangeTypes } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm.js';
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

type Props = {
  config: SearchesConfig,
  noOverride?: boolean,
  toggleDropdownShow: (void) => void,
};

type RangeType = React.Element<Tab>;

const StyledPopover: StyledComponent<{}, void, typeof Popover> = styled(Popover)`
  max-width: 50vw;
  min-width: 745px;
`;

const StyledTabs: StyledComponent<{}, void, typeof Tabs> = styled(Tabs)`
  margin-top: 1px;
`;

const Timezone: StyledComponent<{}, void, HTMLParagraphElement> = styled.p(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  padding-left: 3px;
`);

const timeRangeTypeTabs = (config, activeKey, originalRangeValue) => availableTimeRangeTypes.map<RangeType>(({ type, name }) => {
  const RangeComponent = timeRangeTypes?.[type] || DisabledTimeRangeSelector;

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      {type === activeKey && (
        <RangeComponent config={config}
                        disabled={false}
                        originalTimeRange={originalRangeValue} />
      )}
    </Tab>
  );
});

const TimeRangeDropdown = ({ config, noOverride, toggleDropdownShow }: Props) => {
  const formik = useFormikContext();
  const [originalTimerange, , originalTimerangeHelpers] = useField('timerange');
  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');
  const { value: nextRangeValue } = nextRangeProps;
  const { value: originalRangeValue } = originalTimerange;

  const [activeTab, setActiveTab] = useState(originalRangeValue?.type);

  useEffect(() => {
    if (!nextRangeValue) {
      nextRangeHelpers.setValue(originalRangeValue);
    }
  });

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

  return (
    <StyledPopover id="timerange-type"
                   placement="bottom"
                   positionTop={36}
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
            {timeRangeTypeTabs(config, activeKey, originalRangeValue)}
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
            <Button bsStyle="success" onClick={handleApply}>Apply</Button>
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
