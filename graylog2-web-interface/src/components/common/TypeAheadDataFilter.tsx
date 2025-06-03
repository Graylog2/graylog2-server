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
import isEqual from 'lodash/isEqual';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import type { TypeAheadInputRef } from 'components/common/TypeAheadInput';
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
};

/**
 * Component that renders a data filter input with suggestion capabilities.
 * This component was thought to be able to filter a list of items by one
 * of their attributes, but also on tags, providing auto-completion for them.
 *
 * **Note** There are a few quirks around this component and it will be
 * refactored soon.
 */
class TypeAheadDataFilter extends React.Component<
  TypeAheadDataFilterProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    id: '',
    data: [],
    filterData: undefined,
    label: '',
    onDataFiltered: undefined,
    searchInKeys: [],
  };

  private typeAheadInput: TypeAheadInputRef;

  constructor(props: TypeAheadDataFilterProps) {
    super(props);

    this.state = {
      filterText: '',
    };
  }

  componentDidUpdate(prevProps: TypeAheadDataFilterProps) {
    const { data } = this.props;

    if (!isEqual(prevProps.data, data)) {
      this.filterData();
    }
  }

  _onSearchTextChanged = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.setState({ filterText: this.typeAheadInput.getValue() }, this.filterData);
  };

  _matchStringSearch = (datum) => {
    const { filterText } = this.state;
    const { searchInKeys } = this.props;

    return searchInKeys.some((searchInKey) => {
      const key = datum[searchInKey];
      const value = filterText;

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
    }, this);
  };

  _resetFilters = () => {
    this.typeAheadInput.clear();
    this.setState({ filterText: '' }, this.filterData);
  };

  filterData = () => {
    const { filterData, data, onDataFiltered } = this.props;
    const { filterText } = this.state;

    if (typeof filterData === 'function') {
      return filterData(data);
    }

    const filteredData = data.filter((datum) => this._matchStringSearch(datum), this);

    onDataFiltered(filteredData, filterText);

    return true;
  };

  render() {
    const { filterText } = this.state;
    const { id, label } = this.props;

    return (
      <div className="filter">
        <form
          className="form-inline"
          onSubmit={this._onSearchTextChanged}
          style={{ display: 'inline-flex', alignItems: 'flex-end' }}>
          <TypeAheadInput
            id={id}
            ref={(typeAheadInput) => {
              this.typeAheadInput = typeAheadInput;
            }}
            formGroupClassName=""
            label={label}
          />
          <StyledButton type="submit">Filter</StyledButton>
          <StyledButton type="button" onClick={this._resetFilters} disabled={filterText === ''}>
            Reset
          </StyledButton>
        </form>
      </div>
    );
  }
}

export default TypeAheadDataFilter;
