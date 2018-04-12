import React from 'react';
import PropTypes from 'prop-types';

import Select from 'react-select';
import { SortableContainer, SortableElement, arrayMove } from 'react-sortable-hoc';
import ConfigurablePivot from './ConfigurablePivot';

import { pivotForField } from '../../logic/searchtypes/aggregation/PivotGenerator';

const _onChange = (fields, newValue, onChange) => {
  const newFields = newValue.map(v => v.value);

  return onChange(newFields.map((field) => {
    const existingField = fields.find(f => f.field === field);
    return existingField || pivotForField(field);
  }));
};

const SortableValueList = SortableContainer(({ children, ...rest }) => <span {...rest}>{children}</span>);

const _onSortEnd = ({ oldIndex, newIndex }, onChange, values) => {
  const valuesArray = values.split(',');
  const newItems = arrayMove(valuesArray, oldIndex, newIndex);
  onChange(newItems.join(','));
};

const configFor = ({ value }, values) => values.find(({ field }) => field === value).config;
const newPivotConfigChange = (values, value, newPivotConfig, onChange) => {
  const newValues = values.map((pivot) => {
    if (pivot.field === value.value) {
      return Object.assign({}, pivot, newPivotConfig);
    }
    return pivot;
  });
  return onChange(newValues);
};

const SortableSelect = ({ onChange, value, sortableValueList, ...remainingProps }) => {
  const SortableValue = SortableElement(({ children, ...rest }) => (
    <ConfigurablePivot {...rest}
                       config={configFor(rest.value, value)}
                       onChange={newPivotConfig => newPivotConfigChange(value, rest.value, newPivotConfig, onChange)}>
      {children}
    </ConfigurablePivot>
  ));

  const ValueListComponent = sortableValueList;
  // eslint-disable-next-line react/prop-types
  const valueList = ({ children, ...rest }) => (
    <ValueListComponent {...rest}
                       onSortEnd={v => _onSortEnd(v, onChange, value)}
                       axis="x"
                       pressDelay={200}>
      {children}
    </ValueListComponent>
  );
  const values = value.map(({ field }) => field).join(',');
  return (
    <Select
      multi
      {...remainingProps}
      onChange={s => _onChange(value, s, onChange)}
      value={values}
      valueListComponent={valueList}
      valueComponent={SortableValue}
    />
  );
};

SortableSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  sortableValueList: PropTypes.func,
  value: PropTypes.any.isRequired,
};

SortableSelect.defaultProps = {
  sortableValueList: SortableValueList,
};

export default SortableSelect;
