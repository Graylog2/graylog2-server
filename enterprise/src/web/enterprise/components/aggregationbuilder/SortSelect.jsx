import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { FieldList } from './AggregationBuilderPropTypes';

const SortSelect = ({ fields, onChange, sort }) => (
  <Select placeholder="Sort" size="small" options={fields} multi value={sort} onChange={onChange} />
);

SortSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  sort: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default SortSelect;
