import React from 'react';
import PropTypes from 'prop-types';

import { FieldList } from './AggregationBuilderPropTypes';
import SortableSelect from './SortableSelect';

const SortSelect = ({ fields, onChange, sort }) => (
  <SortableSelect
    placeholder="Sort"
    onChange={onChange}
    options={fields}
    value={sort}
  />
);

SortSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  sort: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default SortSelect;
