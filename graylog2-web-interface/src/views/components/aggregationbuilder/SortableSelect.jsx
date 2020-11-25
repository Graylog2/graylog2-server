/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { components as Components } from 'react-select';
import { SortableContainer, SortableElement } from 'react-sortable-hoc';
import { findIndex } from 'lodash';

import Select from 'views/components/Select';

import styles from './SortableSelect.css';

const SortableSelectContainer = SortableContainer(Select);

const _arrayMove = (array, from, to) => {
  const result = array.slice();

  result.splice(to < 0 ? result.length + to : to, 0, result.splice(from, 1)[0]);

  return result;
};

const _onSortEnd = ({ oldIndex, newIndex }, onChange, values) => {
  const newItems = _arrayMove(values, oldIndex, newIndex);

  onChange(newItems.map(({ field }) => ({ label: field, value: field })));
};

const _defaultValueTransformer = (values) => values.map(({ field }) => ({ value: field, label: field }));

const SortableSelect = ({ onChange, value, valueComponent, valueTransformer, ...remainingProps }) => {
  const values = valueTransformer(value);
  const SortableMultiValue = SortableElement(Components.MultiValue);

  const Item = (props) => {
    // eslint-disable-next-line react/prop-types
    const { data: { value: itemValue } } = props;
    const index = findIndex(value, (v) => v.field === itemValue);

    return <SortableMultiValue index={index} {...props} innerProps={{ title: itemValue }} />;
  };

  const _components = {
    MultiValueLabel: valueComponent,
    MultiValue: Item,
  };

  return (
    <SortableSelectContainer {...remainingProps}
                             isMulti
                             onChange={onChange}
                             value={values}
                             components={_components}
                             onSortEnd={(v) => _onSortEnd(v, onChange, value)}
                             axis="x"
                             helperClass={`Select--multi has-value is-clearable is-searchable ${styles.draggedElement}`}
                             pressDelay={200} />
  );
};

SortableSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.any.isRequired,
  valueComponent: PropTypes.func.isRequired,
  valueTransformer: PropTypes.func,
};

SortableSelect.defaultProps = {
  valueTransformer: _defaultValueTransformer,
};

export default SortableSelect;
