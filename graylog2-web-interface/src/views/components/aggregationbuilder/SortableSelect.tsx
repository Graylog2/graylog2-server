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
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { components as Components } from 'react-select';
import { SortableContainer, SortableElement } from 'react-sortable-hoc';
import { findIndex } from 'lodash';

import Select from 'components/common/Select';

import styles from './SortableSelect.css';

const SortableSelectContainer = SortableContainer(Select);

const _arrayMove = (array: Array<{ field: string }> | undefined, from: number, to: number) => {
  const result = array?.slice() ?? [];

  result.splice(to < 0 ? result.length + to : to, 0, result.splice(from, 1)[0]);

  return result;
};

const _onSortEnd = (
  { oldIndex, newIndex }: { oldIndex: number, newIndex: number },
  onChange: (newOptions: Array<string>) => void,
  values: Array<{ field: string }> | undefined,
) => {
  const newItems = _arrayMove(values, oldIndex, newIndex);

  onChange(newItems.map(({ field }) => field));
};

const _defaultValueTransformer = (values: Array<{ field: string }> | undefined) => values.map(({ field }) => field).join();

type Props = {
  allowOptionCreation?: boolean,
  inputId?: string,
  onChange: (newOptions: Array<string>) => void,
  options: Array<{ label: string, value: string }>,
  value: Array<{ field: string }> | undefined,
  valueComponent: React.ComponentType<any>,
  valueTransformer?: (value: Array<{ field: string }>) => Array<{ value: any, label: string }>,
}
const SortableMultiValue = SortableElement<{ innerProps: { title: string }}>(Components.MultiValue);

const SortableSelect = ({ onChange, value, valueComponent, valueTransformer, inputId, allowOptionCreation, options }: Props) => {
  const values = valueTransformer(value);

  const Item = useCallback((itemProps: { data: { value: string }}) => {
    // eslint-disable-next-line react/prop-types
    const { data: { value: itemValue } } = itemProps;
    const index = findIndex(value, (v) => v.field === itemValue);

    return <SortableMultiValue index={index} {...itemProps} innerProps={{ title: itemValue }} />;
  }, [value]);

  const _components = {
    MultiValueLabel: valueComponent,
    MultiValue: Item,
  };

  return (
    <SortableSelectContainer allowCreate={allowOptionCreation}
                             axis="x"
                             components={_components}
                             helperClass={`Select--multi has-value is-clearable is-searchable ${styles.draggedElement}`}
                             inputId={inputId}
                             multi
                             onChange={(newValue: string = '') => {
                               onChange(newValue.split(','));
                             }}
                             onSortEnd={(v) => _onSortEnd(v, onChange, value)}
                             options={options}
                             pressDelay={200}
                             value={values} />
  );
};

SortableSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.any.isRequired,
  valueComponent: PropTypes.oneOfType([PropTypes.func, PropTypes.object]).isRequired,
  valueTransformer: PropTypes.func,
};

SortableSelect.defaultProps = {
  valueTransformer: _defaultValueTransformer,
  allowOptionCreation: false,
  inputId: undefined,
};

export default SortableSelect;
