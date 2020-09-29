// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { Button, Col, Tabs, Tab, Row, Popover } from 'components/graylog';
import { availableTimeRangeTypes } from 'views/Constants';
import type { SearchesConfig } from 'components/search/SearchConfig';

import TimeRangeInput from '../TimeRangeInput';

type Props = {
  config: SearchesConfig,
  currentType: string,
  onSelect: (newType: string) => void,
  toggleDropdownShow: () => boolean,
};

type RangeType = React.Element<Tab>;

const StyledPopover = styled(Popover)`
  max-width: 50vw;
  min-width: 745px;
`;

const Dropdown = ({ config, currentType, onSelect, toggleDropdownShow }: Props) => {
  const timeRangeTypeTabs = availableTimeRangeTypes.map<RangeType>(({ type, name }) => (
    <Tab title={name}
         key={`time-range-type-selector-${type}`}
         eventKey={type}>
      <TimeRangeInput config={config} disabled={false} />
    </Tab>
  ));

  const handleCancel = () => {
    /** TODO: handleCancel should revert back to previous Search state before anything was toggled */
    console.log('TODO handleCancel');
    toggleDropdownShow();
  };

  const handleApply = () => {
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
                activeKey={currentType}
                onSelect={onSelect}
                animation={false}>
            {timeRangeTypeTabs}
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

export default Dropdown;
