// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { MenuItem } from 'components/graylog';

type Props = {
  type: string;
}
const TimeRangeTypeMenuItems = ({ type }: Props) => (
  <>
    <MenuItem eventKey="relative"
              active={type === 'relative'}>
      Relative
    </MenuItem>
    <MenuItem eventKey="absolute"
              active={type === 'absolute'}>
      Absolute
    </MenuItem>
    <MenuItem eventKey="keyword"
              active={type === 'keyword'}>
      Keyword
    </MenuItem>
  </>
);

TimeRangeTypeMenuItems.propTypes = {
  type: PropTypes.string.isRequired,
};

export default TimeRangeTypeMenuItems;
