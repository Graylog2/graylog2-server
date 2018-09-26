import React from 'react';
import PropTypes from 'prop-types';

import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import PivotSelect from './PivotSelect';

const SortSelect = ({ fields, onChange, sort }) => (
  <PivotSelect
    placeholder="None: click to add fields"
    onChange={onChange}
    options={fields}
    value={sort}
  />
);

SortSelect.propTypes = {
  fields: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.oneOfType([
        PropTypes.string.isRequired,
        PropTypes.instanceOf(Pivot),
      ]),
      value: PropTypes.oneOfType([
        PropTypes.string.isRequired,
        PropTypes.instanceOf(Pivot),
      ]),
    }),
  ).isRequired,
  onChange: PropTypes.func.isRequired,
  sort: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default SortSelect;
