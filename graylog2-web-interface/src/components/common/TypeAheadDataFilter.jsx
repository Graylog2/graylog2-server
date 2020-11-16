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
import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { isEqual } from 'lodash';

import { Button } from 'components/graylog';
import TypeAheadInput from 'components/common/TypeAheadInput';

/**
 * Component that renders a data filter input with suggestion capabilities.
 * This component was thought to be able to filter a list of items by one
 * of their attributes, but also on tags, providing auto-completion for them.
 *
 * **Note** There are a few quirks around this component and it will be
 * refactored soon.
 */
class TypeAheadDataFilter extends React.Component {
  static propTypes = {
    /** ID to use in the filter input field. */
    id: PropTypes.string,
    /**
     * Array of objects to be filtered. Each object must contain at least
     * the keys to be filtered, specified in the `searchInKeys` prop.
     */
    data: PropTypes.array,
    /** Object key to use to display items in the suggestions. */
    displayKey: PropTypes.string,
    /**
     * Object key being used to provide suggestions.
     *
     * **Warning** The key in the data objects is expected to be plural,
     * but here you must give the singular form.
     */
    filterBy: PropTypes.string,
    /**
     * Function to override the default filtering algorithm.
     * @deprecated We never used the function and it seems to be broken,
     * as it cannot filter data if it doesn't received the text introduced
     * in the filter field.
     */
    filterData: PropTypes.func,
    /**
     * Object key where the auto-completion suggestions are stored. Use this
     * if passing an array of objects to `filterSuggestions`.
     */
    filterSuggestionAccessor: PropTypes.string,
    /**
     * Array of strings or objects containing available suggestions to auto
     * complete. If an array of objects is given to this prop, please ensure
     * the `filterSuggestionAccessor` prop specifies which key contains the
     * suggestions.
     */
    filterSuggestions: PropTypes.array,
    /** Label to use for the filter input field. */
    label: PropTypes.string,
    /**
     * Function that will be called when the user changes the filter.
     * The function receives an array of data that matches the filter
     * and filter input value.
     */
    onDataFiltered: PropTypes.func,
    /**
     * Specifies an array of strings containing each key of the data objects
     * that should be compared against the text introduced in the filter
     * input field.
     */
    searchInKeys: PropTypes.array,
  };

  static defaultProps = {
    id: '',
    data: [],
    displayKey: '',
    filterBy: '',
    filterData: undefined,
    filterSuggestionAccessor: '',
    filterSuggestions: [],
    label: '',
    onDataFiltered: undefined,
    searchInKeys: [],
  };

  constructor(props) {
    super(props);
    const { filterBy } = this.props;

    this.state = {
      filterText: '',
      filters: Immutable.OrderedSet(),
      filterByKey: `${filterBy}s`,
    };
  }

  componentDidUpdate(prevProps) {
    const { data } = this.props;

    if (!isEqual(prevProps.data, data)) {
      this.filterData();
    }
  }

  _onSearchTextChanged = (event) => {
    event.preventDefault();
    event.stopPropagation();
    this.setState({ filterText: this.typeAheadInput.getValue() }, this.filterData);
  };

  _onFilterAdded = (event, suggestion) => {
    const { filters } = this.state;
    const { displayKey } = this.props;

    this.setState({
      filters: filters.add(suggestion[displayKey]),
      filterText: '',
    }, this.filterData);

    this.typeAheadInput.clear();
  };

  _onFilterRemoved = (event) => {
    const { filters } = this.state;

    event.preventDefault();
    this.setState({ filters: filters.delete(event.target.getAttribute('data-target')) }, this.filterData);
  };

  _matchFilters = (datum) => {
    const { filters, filterByKey } = this.state;
    const { filterSuggestionAccessor } = this.props;

    return filters.every((filter) => {
      let dataToFilter = datum[filterByKey];

      if (filterSuggestionAccessor) {
        dataToFilter = dataToFilter.map((data) => data[filterSuggestionAccessor].toLocaleLowerCase());
      } else {
        dataToFilter = dataToFilter.map((data) => data.toLocaleLowerCase());
      }

      return dataToFilter.indexOf(filter.toLocaleLowerCase()) !== -1;
    }, this);
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
    this.setState({ filterText: '', filters: Immutable.OrderedSet() }, this.filterData);
  };

  filterData = () => {
    const { filterData, data, onDataFiltered } = this.props;
    const { filterText } = this.state;

    if (typeof filterData === 'function') {
      return filterData(data);
    }

    const filteredData = data.filter((datum) => {
      return this._matchFilters(datum) && this._matchStringSearch(datum);
    }, this);

    onDataFiltered(filteredData, filterText);

    return true;
  };

  render() {
    const { filters, filterText } = this.state;
    const { id, label, displayKey, filterBy, filterSuggestionAccessor, filterSuggestions } = this.props;
    const filtersContent = filters.map((filter) => {
      return (
        <li key={`li-${filter}`}>
          <span className="pill label label-default">
            {filterBy}: {filter}
            <button type="button" className="tag-remove" data-target={filter} onClick={this._onFilterRemoved} aria-label={`Remove filter ${filter}`} />
          </span>
        </li>
      );
    });

    let suggestions;

    if (filterSuggestionAccessor) {
      suggestions = filterSuggestions.map((filterSuggestion) => filterSuggestion[filterSuggestionAccessor].toLocaleLowerCase());
    } else {
      suggestions = filterSuggestions.map((filterSuggestion) => filterSuggestion.toLocaleLowerCase());
    }

    suggestions.filter((filterSuggestion) => !filters.includes(filterSuggestion));

    return (
      <div className="filter">
        <form className="form-inline" onSubmit={this._onSearchTextChanged} style={{ display: 'inline-flex', alignItems: 'flex-end' }}>
          <TypeAheadInput id={id}
                          ref={(typeAheadInput) => { this.typeAheadInput = typeAheadInput; }}
                          onSuggestionSelected={this._onFilterAdded}
                          formGroupClassName=""
                          suggestionText={`Filter by ${filterBy}: `}
                          suggestions={suggestions}
                          label={label}
                          displayKey={displayKey} />
          <Button type="submit" style={{ marginLeft: 5 }}>Filter</Button>
          <Button type="button"
                  style={{ marginLeft: 5 }}
                  onClick={this._resetFilters}
                  disabled={filters.count() === 0 && filterText === ''}>
            Reset
          </Button>
        </form>
        <ul className="pill-list">
          {filtersContent}
        </ul>
      </div>
    );
  }
}

export default TypeAheadDataFilter;
