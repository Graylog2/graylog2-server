// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { useState } from 'react';
import { useFormikContext, useField } from 'formik';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { availableTimeRangeTypes } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from './KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from './RelativeTimeRangeSelector';
import DisabledTimeRangeSelector from './DisabledTimeRangeSelector';

import { migrateTimeRangeToNewType } from '../../TimerangeForForm';

const timeRangeTypes = {
  absolute: AbsoluteTimeRangeSelector,
  relative: RelativeTimeRangeSelector,
  keyword: KeywordTimeRangeSelector,
};

type Props = {
  config: SearchesConfig,
  noOverride?: boolean,
  toggleDropdownShow: () => boolean,
};

type RangeType = React.Element<Tab>;

const StyledPopover = styled(Popover)`
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
  const originalTimerange = useField('timerange')[0];
  const originalTimerangeHelpers = useField('timerange')[2];
  const nextRangeProps = useField('temp')[0];
  const nextRangeHelpers = useField('temp')[2];
  const [activeTab, setActiveTab] = useState(originalTimerange?.value?.type);

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
    formik.unregisterField('temp');
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
