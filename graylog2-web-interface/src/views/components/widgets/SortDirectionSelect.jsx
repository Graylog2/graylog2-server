// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';

import Direction from 'views/logic/aggregationbuilder/Direction';

type Props = {
  direction: string,
  disabled: boolean,
  onChange: (Direction) => any,
};

const valueContainer = base => ({
  ...base,
  minHeight: '35px',
});

const SortDirectionSelect = ({ direction, disabled, onChange }: Props): Select => (
  <Select isDisabled={disabled}
          isClearable={false}
          isSearchable={false}
          options={[
            { label: 'Ascending', value: 'Ascending' },
            { label: 'Descending', value: 'Descending' },
          ]}
          onChange={({ value }) => onChange(Direction.fromString(value))}
          placeholder={disabled ? 'No sorting selected' : 'Click to select direction'}
          styles={{ valueContainer }}
          value={direction && { label: direction, value: direction }} />
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
