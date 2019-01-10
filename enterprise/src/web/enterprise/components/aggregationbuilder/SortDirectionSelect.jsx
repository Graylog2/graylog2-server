import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import Direction from '../../logic/aggregationbuilder/Direction';

const SortDirectionSelect = ({ direction, disabled, onChange }) => (
  <Select
    disabled={disabled}
    options={[
      { label: 'Ascending', value: 'Ascending' },
      { label: 'Descending', value: 'Descending' },
    ]}
    onChange={newValue => onChange(Direction.fromString(newValue))}
    placeholder="None: Click to select sort direction"
    simpleValue
    value={direction}
    clearable={false}
    searchable={false}
  />
);

SortDirectionSelect.propTypes = {
  direction: PropTypes.string,
  disabled: PropTypes.bool,
  onChange: PropTypes.func,
};

SortDirectionSelect.defaultProps = {
  direction: undefined,
  disabled: false,
  onChange: () => {},
};

export default SortDirectionSelect;
