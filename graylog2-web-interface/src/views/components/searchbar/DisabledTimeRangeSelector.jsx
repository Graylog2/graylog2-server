// @flow strict
import * as React from 'react';
import Input from 'components/bootstrap/Input';

const DisabledTimeRangeSelector = () => (
  <div className="timerange-selector relative"
       style={{ marginLeft: 50 }}>
    <Input id="relative-timerange-selector"
           type="select"
           disabled
           className="relative"
           value="disabled"
           name="relative">
      <option value="disabled">No Override</option>
    </Input>
  </div>
);

DisabledTimeRangeSelector.propTypes = {};

export default DisabledTimeRangeSelector;
