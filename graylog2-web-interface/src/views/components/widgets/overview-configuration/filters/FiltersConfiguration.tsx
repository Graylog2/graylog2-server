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
import type * as Immutable from 'immutable';
import { Field } from 'formik';

import FilterSelect
  from 'views/components/widgets/overview-configuration/filters/FilterSelect';

import SelectedFiltersList from './SelectedFiltersList';
import type { FilterComponents, Filter } from './types';

type Props = {
  columnTitle: (column: string) => string,
  filterComponents: FilterComponents,
  name: string
}

const FiltersConfiguration = ({ filterComponents, columnTitle, name }: Props) => (
  <div>
    <Field name={name}>
      {({ field: { value: values, onChange } }) => {
        const changeField = (filtersValue: Immutable.OrderedSet<Filter>) => onChange({ target: { value: filtersValue, name } });

        const onCreate = (column: string, value: string) => {
          const filterList = values.toList();
          const filterIndex = values.toList().findIndex(({ field }) => field === column);

          if (filterIndex >= 0) {
            const existingColumnFilter = filterList.get(filterIndex);
            const newColumnFilter = { ...existingColumnFilter };

            if (!newColumnFilter.value.includes(value)) {
              newColumnFilter.value = [...newColumnFilter.value, value];

              return changeField(filterList.set(filterIndex, newColumnFilter).toOrderedSet());
            }
          }

          return changeField(filterList.push({ field: column, value: [value] }).toOrderedSet());
        };

        const onDelete = (filterIndex: number, value: string) => {
          const filterList = values.toList();
          const filter = filterList.get(filterIndex);

          if (filter.value.length === 1) {
            return changeField(filterList.delete(filterIndex).toOrderedSet());
          }

          const newFilter = { ...filter, value: filter.value.filter((val) => val !== value) };

          return changeField(filterList.set(filterIndex, newFilter).toOrderedSet());
        };

        const onEdit = (filterIndex: number, valueIndex: number, newValue: string) => {
          const filterList = values.toList();
          const filter = { ...filterList.get(filterIndex) };
          const newFilterValue = [...filter.value];

          newFilterValue[valueIndex] = newValue;
          filter.value = newFilterValue;

          changeField(filterList.set(filterIndex, filter).toOrderedSet());
        };

        return (
          <>
            <SelectedFiltersList selectedFilters={values}
                                 columnTitle={columnTitle}
                                 onDelete={onDelete}
                                 onEdit={onEdit}
                                 filterComponents={filterComponents} />
            <FilterSelect selectedFilters={values}
                          filterComponents={filterComponents}
                          columnTitle={columnTitle}
                          onCreate={onCreate} />
          </>
        );
      }}
    </Field>
  </div>
);

export default FiltersConfiguration;
