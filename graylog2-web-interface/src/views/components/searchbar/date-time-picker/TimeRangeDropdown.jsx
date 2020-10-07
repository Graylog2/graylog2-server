// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { useEffect, useState } from 'react';
import { useFormikContext, useField } from 'formik';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { availableTimeRangeTypes } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { migrateTimeRangeToNewType } from 'views/components/TimerangeForForm.js';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from './KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from './RelativeTimeRangeSelector';
import DisabledTimeRangeSelector from './DisabledTimeRangeSelector';

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

const timeRangeTypeTabs = (config) => availableTimeRangeTypes.map<RangeType>(({ type, name }) => {
  const RangeComponent = timeRangeTypes?.[type] || DisabledTimeRangeSelector;

  return (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      <RangeComponent config={config} disabled={false} />
    </Tab>
  );
});

const TimeRangeDropdown = ({ config, noOverride, toggleDropdownShow }: Props) => {
  const formik = useFormikContext();
  const [originalTimerange, , originalTimerangeHelpers] = useField('timerange');
  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');
  const [activeTab, setActiveTab] = useState(originalTimerange?.value?.type);

  useEffect(() => {
    nextRangeHelpers.setValue(originalTimerange.value, originalTimerange.value);
  });

  const onSelect = (newType) => {
    setActiveTab(newType);
    nextRangeHelpers.setValue(migrateTimeRangeToNewType(originalTimerange.value, newType));
  };

  const handleCancel = () => {
    nextRangeHelpers.setValue(originalTimerange.value);
    toggleDropdownShow();
  };

  const handleApply = () => {
    originalTimerangeHelpers.setValue(nextRangeProps?.value || {});
    formik.unregisterField('tempTimeRange');
    toggleDropdownShow();
  };

  return (
    <StyledPopover id="timerange-type"
                   placement="bottom"
                   positionTop={36}
                   arrowOffsetLeft={34}>
      <Row>
        <Col md={12}>
          <Tabs id="dateTimeTypes"
                defaultActiveKey={availableTimeRangeTypes[0].type}
                activeKey={activeTab || 'disabled'}
                onSelect={onSelect}
                animation={false}>
            {noOverride && (
              <Tab title="No Override"
                   key="time-range-type-selector-disabled"
                   eventKey="disabled">
                <p>No Override to Date.</p>
              </Tab>
            )}
            {timeRangeTypeTabs(config)}
          </Tabs>
        </Col>
      </Row>

      <Row className="row-sm">
        <Col md={12}>
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
