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
import { useCallback, useEffect, useRef, useState } from 'react';
import debounce from 'lodash/debounce';
import isEqual from 'lodash/isEqual';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import type { TypeAheadInputOnKeyDown, TypeAheadInputRef } from 'components/common/TypeAheadInput';
import TypeAheadInput from 'components/common/TypeAheadInput';

const StyledButton = styled(Button)`
  margin-left: 5px;
`;

type TypeAheadDataFilterProps = {
  /** ID to use in the filter input field. */
  id?: string;
  /**
   * Array of objects to be filtered. Each object must contain at least
   * the keys to be filtered, specified in the `searchInKeys` prop.
   */
  data?: any[];
  /**
   * Function to override the default filtering algorithm.
   * @deprecated We never used the function and it seems to be broken,
   * as it cannot filter data if it doesn't received the text introduced
   * in the filter field.
   */
  filterData?: (...args: any[]) => void;
  /** Label to use for the filter input field. */
  label?: string;
  /**
   * Function that will be called when the user changes the filter.
   * The function receives an array of data that matches the filter
   * and filter input value.
   */
  onDataFiltered?: (...args: any[]) => void;
  /**
   * Specifies an array of strings containing each key of the data objects
   * that should be compared against the text introduced in the filter
   * input field.
   */
  searchInKeys?: any[];
  /** Delay in milliseconds before filtering while typing. */
  debounceMs?: number;
};

/**
 * Component that renders a data filter input with suggestion capabilities.
 * This component was thought to be able to filter a list of items by one
 * of their attributes, but also on tags, providing auto-completion for them.
 *
 * **Note** There are a few quirks around this component and it will be
 * refactored soon.
 */
const TypeAheadDataFilter = ({
  id = '',
  data = [],
  filterData: filterDataOverride = undefined,
  label = '',
  onDataFiltered = undefined,
  searchInKeys = [],
  debounceMs = 250,
}: TypeAheadDataFilterProps) => {
  const [filterText, setFilterText] = useState('');
  const filterTextRef = useRef('');
  const previousDataRef = useRef<any[] | undefined>(data);
  const typeAheadInputRef = useRef<TypeAheadInputRef | null>(null);

  const setFilterTextValue = useCallback((value: string) => {
    filterTextRef.current = value;
    setFilterText(value);
  }, []);

  const matchStringSearch = useCallback(
    (datum) =>
      searchInKeys.some((searchInKey) => {
        const key = datum[searchInKey];
        const value = filterTextRef.current;

        if (key === null) {
          return false;
        }

        const containsFilter = (entry, thisValue) => {
          if (typeof entry === 'undefined') {
            return false;
          }

          return entry.toLocaleLowerCase().indexOf(thisValue.toLocaleLowerCase()) !== -1;
        };

        if (typeof key === 'object') {
          return key.some((arrayEntry) => containsFilter(arrayEntry, value));
        }

        return containsFilter(key, value);
      }),
    [searchInKeys],
  );

  const filterData = useCallback(() => {
    if (typeof filterDataOverride === 'function') {
      return filterDataOverride(data);
    }

    const filteredData = data.filter((datum) => matchStringSearch(datum));
    const currentFilterText = filterTextRef.current;

    onDataFiltered(filteredData, currentFilterText);

    return true;
  }, [data, filterDataOverride, matchStringSearch, onDataFiltered]);

  const filterDataRef = useRef(filterData);
  useEffect(() => {
    filterDataRef.current = filterData;
  }, [filterData]);

  const debouncedFilterDataRef = useRef<ReturnType<typeof debounce> | null>(null);
  useEffect(() => {
    const debounced = debounce(() => filterDataRef.current(), debounceMs);
    debouncedFilterDataRef.current = debounced;

    return () => debounced.cancel();
  }, [debounceMs]);

  useEffect(() => {
    if (!isEqual(previousDataRef.current, data)) {
      previousDataRef.current = data;
      filterDataRef.current();
    }
  }, [data]);

  const onFilterTextChanged = useCallback(
    (value: string) => {
      setFilterTextValue(value);
      const debounced = debouncedFilterDataRef.current;
      if (debounced) {
        debounced();

        return;
      }

      filterDataRef.current();
    },
    [setFilterTextValue],
  );

  const applyFilters = useCallback(() => {
    const debounced = debouncedFilterDataRef.current;
    if (debounced) {
      debounced.flush();

      return;
    }

    filterDataRef.current();
  }, []);

  const onFilterKeyDown: TypeAheadInputOnKeyDown = useCallback(
    (event) => {
      if (event.key === 'Enter') {
        event.preventDefault();
        event.stopPropagation();
        applyFilters();
      }
    },
    [applyFilters],
  );

  const resetFilters = useCallback(() => {
    debouncedFilterDataRef.current?.cancel();
    typeAheadInputRef.current?.clear();
    setFilterTextValue('');
    filterDataRef.current();
  }, [setFilterTextValue]);

  return (
    <div className="filter">
      <div className="form-inline" style={{ display: 'inline-flex', alignItems: 'flex-end' }} role="search">
        <TypeAheadInput
          id={id}
          ref={typeAheadInputRef}
          formGroupClassName=""
          label={label}
          onChange={onFilterTextChanged}
          onKeyDown={onFilterKeyDown}
        />
        <StyledButton type="button" onClick={applyFilters}>
          Filter
        </StyledButton>
        <StyledButton type="button" onClick={resetFilters} disabled={filterText === ''}>
          Reset
        </StyledButton>
      </div>
    </div>
  );
};

export default TypeAheadDataFilter;
