// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import * as Immutable from 'immutable';

import { SortList } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import CustomPropTypes from 'views/components/CustomPropTypes';

import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { defaultCompare } from 'views/logic/DefaultCompare';

type Props = {
  fields: Immutable.Set<FieldTypeMapping>,
  onChange: (Array<*>) => any,
  sort: Array<SortConfig>,
};

const currentValue = (sort, options) => {
  const currentOption = sort && sort.length > 0 && options.find(option => option.label === sort[0].field);
  if (currentOption === undefined) {
    return undefined;
  }
  return currentOption;
};

const FieldSortSelect = ({ fields, onChange, sort }: Props) => {
  const options = fields.sort((field1, field2) => defaultCompare(field1.name, field2.name)).map((field, idx) => ({ label: field.name, value: idx }));
  const _onChange = (newValue, reason) => {
    if (reason.action === 'clear') {
      return onChange([]);
    }
    const { value } = newValue;
    const option = options.find(pt => pt.value === value);
    if (!option) {
      return undefined;
    }
    const sortConfig = new SortConfig('pivot', option.label, Direction.Ascending);
    return onChange([sortConfig]);
  };
  return (
    <Select placeholder="None: click to add fields"
            onChange={_onChange}
            options={options}
            isClearable
            value={currentValue(sort, options)} />
  );
};

FieldSortSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  sort: SortList.isRequired,
};

export default FieldSortSelect;
