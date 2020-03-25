// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import * as Immutable from 'immutable';

import CustomPropTypes from 'views/components/CustomPropTypes';

import { defaultCompare } from 'views/logic/DefaultCompare';
import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

type Props = {
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (Array<*>) => any,
  sort: Array<SortConfig>,
};

type Option = {
  label: string,
  value: number
}

const findOptionByLabel = (options: Immutable.List<Option>, label: string) => options.find(option => option.label === label);

const findOptionByValue = (options: Immutable.List<Option>, value: number) => options.find(option => option.value === value);

const currentValue = (sort: Array<SortConfig>, options: Immutable.List<Option>) => sort && sort.length > 0 && findOptionByLabel(options, sort[0].field);

const sortedOptions = (fields: Immutable.List<FieldTypeMapping>) => {
  return fields.sort(
    (field1, field2) => defaultCompare(field1.name, field2.name),
  ).map(
    (field, idx) => ({ label: field.name, value: idx }),
  );
};

const onOptionChange = (options: Immutable.List<Option>, onChange, newValue, reason) => {
  if (reason.action === 'clear') {
    return onChange([]);
  }
  const { value } = newValue;
  const option = findOptionByValue(options, value);
  if (!option) {
    return undefined;
  }
  const sortConfig = new SortConfig(SortConfig.PIVOT_TYPE, option.label, Direction.Ascending);
  return onChange([sortConfig]);
};

const FieldSortSelect = ({ fields, onChange, sort }: Props) => {
  const options: Immutable.List<Option> = sortedOptions(fields);
  return (
    <Select placeholder="None: click to add fields"
            onChange={(newValue, reason) => onOptionChange(options, onChange, newValue, reason)}
            options={options}
            isClearable
            value={currentValue(sort, options)} />
  );
};

FieldSortSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  sort: PropTypes.array.isRequired,
};

export default FieldSortSelect;
