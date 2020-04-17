// @flow strict
import * as React from 'react';
import Input from 'components/bootstrap/Input';

const DisabledTimeRangeSelector = () => (
  <div className="timerange-selector"
       style={{ marginLeft: 50 }}>
    <Input id="no-override-timerange-selector"
           type="select"
           disabled
           value="disabled"
           title="There is no override for the timerange currently selected"
           name="no-override">
      <option value="disabled">No Override</option>
    </Input>
  </div>
);

DisabledTimeRangeSelector.propTypes = {};

export default DisabledTimeRangeSelector;
