// @flow strict
import * as React from 'react';

import { MenuItem } from 'components/graylog';
import { availableTimeRangeTypes } from 'views/Constants';

type ResultType = React.Element<MenuItem>;

const timeRangeTypeMenuItems = (currentType: string) => availableTimeRangeTypes.map<ResultType>(({ type, name }) => (
  <MenuItem key={`time-range-type-selector-${type}`}
            eventKey={type}
            active={currentType === type}>
    {name}
  </MenuItem>
));

export default timeRangeTypeMenuItems;
