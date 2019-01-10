import React from 'react';
import PropTypes from 'prop-types';

import Select from 'react-select';
import { SortableContainer, SortableElement, arrayMove } from 'react-sortable-hoc';

import styles from './SortableSelect.css';

const SortableValueList = SortableContainer(({ children, ...rest }) => <span {...rest}>{children}</span>);

const _onSortEnd = ({ oldIndex, newIndex }, onChange, values) => {
  const valuesArray = values.split(',');
  const newItems = arrayMove(valuesArray, oldIndex, newIndex);
  onChange(newItems.join(','));
};

const defaultValueTransformer = values => values.map(({ field }) => field).join(',');

const SortableSelect = ({ onChange, value, valueComponent, sortableValueList, valueTransformer, ...remainingProps }) => {
  const ValueListComponent = sortableValueList;
  // eslint-disable-next-line react/prop-types
  const valueList = ({ children, ...rest }) => (
    <ValueListComponent {...rest}
                        onSortEnd={v => _onSortEnd(v, onChange, value)}
                        axis="x"
                        helperClass={`Select--multi has-value is-clearable is-searchable ${styles.draggedElement}`}
                        pressDelay={200}>
      {children}
    </ValueListComponent>
  );
  const values = valueTransformer(value);
  return (
    <Select
      multi
      {...remainingProps}
      onChange={onChange}
      value={values}
      valueListComponent={valueList}
      valueComponent={SortableElement(valueComponent)}
    />
  );
};

SortableSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  sortableValueList: PropTypes.func,
  value: PropTypes.any.isRequired,
  valueComponent: PropTypes.func.isRequired,
  valueTransformer: PropTypes.func,
};

SortableSelect.defaultProps = {
  sortableValueList: SortableValueList,
  valueTransformer: defaultValueTransformer,
};

export default SortableSelect;
