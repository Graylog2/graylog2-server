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
import ImmutablePropTypes from 'react-immutable-proptypes';
import lodash from 'lodash';

import { Col, Row } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ControlledTableList from 'components/common/ControlledTableList';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';

import style from './TableList.css';

/**
 * Component that renders a list of items in a table-like structure. The list
 * also includes a filter input that can be used to search for specific items
 * or elements matching a string.
 *
 * The component can render action elements for each item and (optionally) for
 * performing bulk operations. In that second case, action elements will
 * appear in the header once the user selects one or more items by checking
 * the checkboxes next to them.
 *
 * This component uses `ControlledTableList` underneath. If you need to further
 * customize how the list of components should look like, or how filtering works,
 * please use that component instead of this one.
 */
class TableList extends React.Component {
  static propTypes = {
    /** Specifies key to use as item ID. */
    idKey: PropTypes.string,
    /** Specifies a key to use as item title. */
    titleKey: PropTypes.string,
    /** Specifies key to use as item description. */
    descriptionKey: PropTypes.string,
    /** Indicates whether the component should render a filter or not. */
    enableFilter: PropTypes.bool,
    /** Object keys to use for filtering. */
    filterKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    /** Label to use next to the filter input. */
    filterLabel: PropTypes.string,
    /** Hide description */
    hideDescription: PropTypes.bool,
    /**
     * Immutable List of objects to display in the list. Objects are expected
     * to have an ID (`idKey` prop), a title (`title` prop), and an optional
     * description (`descriptionKey` prop).
     */
    items: ImmutablePropTypes.list.isRequired,
    /** Indicates whether the component will enable bulk actions or not. */
    enableBulkActions: PropTypes.bool,
    /**
     * Function that generates react elements to render in the header.
     * Those elements are meant to display actions that affect more than one
     * item in the list, so they will only be displayed when one or more items
     * are checked.
     * The function receives a list of IDs corresponding to all selected
     * elements as argument.
     * This function will not be called if `enableBulkActions` is set to `false`.
     */
    bulkActionsFactory: PropTypes.func,
    /**
     * Function that generates react elements to render for each item.
     * Those elements are meant to display actions that affect that specific
     * item.
     * The function will receive the whole item object as an argument.
     */
    itemActionsFactory: PropTypes.func,
  };

  static defaultProps = {
    idKey: 'id',
    titleKey: 'title',
    descriptionKey: 'description',
    enableFilter: true,
    filterLabel: 'Filter',
    enableBulkActions: true,
    bulkActionsFactory: () => {},
    itemActionsFactory: () => {},
  };

  state = {
    filteredItems: this.props.items,
    selected: Immutable.Set(),
  };

  componentDidUpdate(prevProps) {
    const { filteredItems, selected } = this.state;

    this._setSelectAllCheckboxState(this.selectAllInput, filteredItems, selected);

    if (!this.props.items.equals(prevProps.items)) {
      if (this.props.enableFilter) {
        // This will apply the current filter to new items and update the state
        this.filter.filterData();
      } else {
        this._updateFilteredItems(this.props.items);
      }
    }
  }

  _recalculateSelection = (selected, nextFilteredItems) => {
    const nextFilteredIds = Immutable.Set(nextFilteredItems.map((item) => item[this.props.idKey]));

    return selected.intersect(nextFilteredIds);
  };

  _updateFilteredItems = (nextFilteredItems) => {
    const filteredSelected = this._recalculateSelection(this.state.selected, nextFilteredItems);

    this.setState({ filteredItems: nextFilteredItems, selected: filteredSelected });
  };

  _setSelectAllCheckboxState = (selectAllInput, filteredItems, selected) => {
    const selectAllCheckbox = selectAllInput ? selectAllInput.getInputDOMNode() : undefined;

    if (!selectAllCheckbox) {
      return;
    }

    // Set the select all checkbox as indeterminate if some but not items are selected.
    selectAllCheckbox.indeterminate = selected.count() > 0 && !this._isAllSelected(filteredItems, selected);
  };

  _filterItems = (filteredItems) => {
    this._updateFilteredItems(Immutable.List(filteredItems));
  };

  _isAllSelected = (filteredItems, selected) => {
    return filteredItems.count() > 0 && filteredItems.count() === selected.count();
  };

  _headerItem = () => {
    if (!this.props.enableBulkActions) {
      return <ControlledTableList.Header />;
    }

    const { filteredItems, selected } = this.state;
    const selectedItems = selected.count();

    return (
      <ControlledTableList.Header>
        {selectedItems > 0
        && (
        <div className={style.headerComponentsWrapper}>
          {this.props.bulkActionsFactory(selected)}
        </div>
        )}
        <Input ref={(c) => { this.selectAllInput = c; }}
               id="select-all-checkbox"
               type="checkbox"
               label={selectedItems === 0 ? 'Select all' : `${selectedItems} selected`}
               disabled={filteredItems.count() === 0}
               checked={this._isAllSelected(filteredItems, selected)}
               onChange={this._toggleSelectAll}
               wrapperClassName="form-group-inline" />
      </ControlledTableList.Header>
    );
  };

  _toggleSelectAll = (event) => {
    const newSelected = event.target.checked ? Immutable.Set(this.state.filteredItems.map((item) => item[this.props.idKey])) : Immutable.Set();

    this.setState({ selected: newSelected });
  };

  _formatItem = (item) => {
    let formattedItem;

    if (this.props.enableBulkActions) {
      formattedItem = (
        <Input id={`${this.props.idKey}-checkbox`}
               type="checkbox"
               label={item[this.props.titleKey]}
               checked={this.state.selected.includes(item[this.props.idKey])}
               onChange={this._onItemSelect(item[this.props.idKey])}
               wrapperClassName="form-group-inline" />
      );
    } else {
      formattedItem = <div id={`${this.props.idKey}-input`} className="header">{item[this.props.titleKey]}</div>;
    }

    return (
      <ControlledTableList.Item key={`item-${item[this.props.idKey]}`}>
        <div className={`${style.itemWrapper} ${this.props.enableBulkActions ? '' : style.itemWrapperStatic}`}>
          <div className={style.itemActionsWrapper}>
            {this.props.itemActionsFactory(item)}
          </div>

          {formattedItem}
          {this.props.hideDescription ? null : <span className="description">{item[this.props.descriptionKey]}</span>}
        </div>
      </ControlledTableList.Item>
    );
  };

  _onItemSelect = (id) => {
    return (event) => {
      const newSelected = event.target.checked ? this.state.selected.add(id) : this.state.selected.delete(id);

      this.setState({ selected: newSelected });
    };
  };

  render() {
    let filter;

    if (this.props.enableFilter) {
      filter = (
        <Row>
          <Col md={12}>
            <TypeAheadDataFilter ref={(c) => { this.filter = c; }}
                                 id={`${lodash.kebabCase(this.props.filterLabel)}-data-filter`}
                                 label={this.props.filterLabel}
                                 data={this.props.items.toJS()}
                                 displayKey="value"
                                 filterSuggestions={[]}
                                 searchInKeys={this.props.filterKeys}
                                 onDataFiltered={this._filterItems} />
          </Col>
        </Row>
      );
    }

    let formattedItems;

    if (this.props.items.count() === 0) {
      formattedItems = (
        <ControlledTableList.Item>No items to display</ControlledTableList.Item>
      );
    } else if (this.state.filteredItems.count() === 0) {
      formattedItems = (
        <ControlledTableList.Item>No items match your filter criteria</ControlledTableList.Item>
      );
    } else {
      formattedItems = this.state.filteredItems.map((item) => this._formatItem(item)).toJS();
    }

    return (
      <div>
        {filter}
        <ControlledTableList>
          {this._headerItem()}
          {formattedItems}
        </ControlledTableList>
      </div>
    );
  }
}

export default TableList;
