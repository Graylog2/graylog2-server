import React from 'react';
import PropTypes from 'prop-types';

import { components as Components } from 'react-select';
import { SortableContainer, SortableElement } from 'react-sortable-hoc';
import { findIndex } from 'lodash';

import Select from 'views/components/Select';
import styles from './SortableSelect.css';

const SortableValueList = SortableContainer(Components.ValueContainer);

const arrayMove = (array, from, to) => {
  const result = array.slice();
  result.splice(to < 0 ? result.length + to : to, 0, result.splice(from, 1)[0]);
  return result;
};

const _onSortEnd = ({ oldIndex, newIndex }, onChange, values) => {
  const newItems = arrayMove(values, oldIndex, newIndex);
  onChange(newItems.map(({ field }) => ({ label: field, value: field })));
};

const defaultValueTransformer = values => values.map(({ field }) => ({ value: field, label: field }));

const SortableSelect = ({ onChange, value, valueComponent, valueTransformer, ...remainingProps }) => {
  // eslint-disable-next-line react/prop-types
  const ValueList = ({ children, ...rest }) => (
    <SortableValueList {...rest}
                       onSortEnd={v => _onSortEnd(v, onChange, value)}
                       axis="x"
                       helperClass={`Select--multi has-value is-clearable is-searchable ${styles.draggedElement}`}
                       pressDelay={200}>
      {children}
    </SortableValueList>
  );
  const values = valueTransformer(value);
  const SortableMultiValue = SortableElement(Components.MultiValue);
  const Item = (props: {data: {value: string}}) => {
    const { data: { value: itemValue } } = props;
    const index = findIndex(value, v => v.field === itemValue);
    return <SortableMultiValue index={index} {...props} innerProps={{ title: itemValue }} />;
  };
  const _components = {
    MultiValueLabel: valueComponent,
    MultiValue: Item,
    ValueContainer: ValueList,
  };
  return (
    <Select isMulti
            {...remainingProps}
            onChange={onChange}
            value={values}
            components={_components} />
  );
};

SortableSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.any.isRequired,
  valueComponent: PropTypes.func.isRequired,
  valueTransformer: PropTypes.func,
};

SortableSelect.defaultProps = {
  valueTransformer: defaultValueTransformer,
};

export default SortableSelect;
