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
     * The function receives an array of data that matches the filter.
     */
    onDataFiltered: PropTypes.func,
    /**
     * Specifies an array of strings containing each key of the data objects
     * that should be compared against the text introduced in the filter
     * input field.
     */
    searchInKeys: PropTypes.array,
  };

  state = {
    filterText: '',
    filters: Immutable.OrderedSet(),
    filterByKey: `${this.props.filterBy}s`,
  };
  componentDidUpdate(prevProps) {
    if (!isEqual(prevProps.data, this.props.data)) {
      this.filterData();
    }
  }

  _onSearchTextChanged = (event) => {
    event.preventDefault();
    this.setState({ filterText: this.typeAheadInput.getValue() }, this.filterData);
  };

  _onFilterAdded = (event, suggestion) => {
    this.setState({
      filters: this.state.filters.add(suggestion[this.props.displayKey]),
      filterText: '',
    }, this.filterData);
    this.typeAheadInput.clear();
  };

  _onFilterRemoved = (event) => {
    event.preventDefault();
    this.setState({ filters: this.state.filters.delete(event.target.getAttribute('data-target')) }, this.filterData);
  };

  _matchFilters = (datum) => {
    return this.state.filters.every((filter) => {
      let dataToFilter = datum[this.state.filterByKey];

      if (this.props.filterSuggestionAccessor) {
        dataToFilter = dataToFilter.map(data => data[this.props.filterSuggestionAccessor].toLocaleLowerCase());
      } else {
        dataToFilter = dataToFilter.map(data => data.toLocaleLowerCase());
      }

      return dataToFilter.indexOf(filter.toLocaleLowerCase()) !== -1;
    }, this);
  };

  _matchStringSearch = (datum) => {
    return this.props.searchInKeys.some((searchInKey) => {
      const key = datum[searchInKey];
      const value = this.state.filterText;

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
        return key.some(arrayEntry => containsFilter(arrayEntry, value));
      }
      return containsFilter(key, value);
    }, this);
  };

  _resetFilters = () => {
    this.typeAheadInput.clear();
    this.setState({ filterText: '', filters: Immutable.OrderedSet() }, this.filterData);
  };

  filterData = () => {
    if (typeof this.props.filterData === 'function') {
      return this.props.filterData(this.props.data);
    }

    const filteredData = this.props.data.filter((datum) => {
      return this._matchFilters(datum) && this._matchStringSearch(datum);
    }, this);

    this.props.onDataFiltered(filteredData);
  };

  render() {
    const filters = this.state.filters.map((filter) => {
      return (
        <li key={`li-${filter}`}>
          <span className="pill label label-default">
            {this.props.filterBy}: {filter}
            <a className="tag-remove" data-target={filter} onClick={this._onFilterRemoved} />
          </span>
        </li>
      );
    });

    let suggestions;

    if (this.props.filterSuggestionAccessor) {
      suggestions = this.props.filterSuggestions.map(filterSuggestion => filterSuggestion[this.props.filterSuggestionAccessor].toLocaleLowerCase());
    } else {
      suggestions = this.props.filterSuggestions.map(filterSuggestion => filterSuggestion.toLocaleLowerCase());
    }

    suggestions.filter(filterSuggestion => !this.state.filters.includes(filterSuggestion));

    return (
      <div className="filter">
        <form className="form-inline" onSubmit={this._onSearchTextChanged} style={{ display: 'inline' }}>
          <TypeAheadInput id={this.props.id}
                          ref={(typeAheadInput) => { this.typeAheadInput = typeAheadInput; }}
                          onSuggestionSelected={this._onFilterAdded}
                          suggestionText={`Filter by ${this.props.filterBy}: `}
                          suggestions={suggestions}
                          label={this.props.label}
                          displayKey={this.props.displayKey} />
          <Button type="submit" style={{ marginLeft: 5 }}>Filter</Button>
          <Button type="button"
                  style={{ marginLeft: 5 }}
                  onClick={this._resetFilters}
                  disabled={this.state.filters.count() === 0 && this.state.filterText === ''}>
            Reset
          </Button>
        </form>
        <ul className="pill-list">
          {filters}
        </ul>
      </div>
    );
  }
}

export default TypeAheadDataFilter;
