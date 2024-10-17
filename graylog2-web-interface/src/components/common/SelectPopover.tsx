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
import concat from 'lodash/concat';
import isEqual from 'lodash/isEqual';
import without from 'lodash/without';
import IsolatedScroll from 'react-isolated-scroll';

import {
  FormControl,
  FormGroup,
  ListGroup,
  ListGroupItem,
} from 'components/bootstrap';
import Icon from 'components/common/Icon';
import OverlayTrigger from 'components/common/OverlayTrigger';

import style from './SelectPopover.css';

type SelectPopoverProps = {
  /** Provides an ID for this popover element. */
  id: string;
  /** Indicates where the popover should appear. */
  placement?: 'top' | 'right' | 'bottom' | 'left';
  /** Title to use in the popover header. */
  title: string;
  /** React node that will be used as trigger to show/hide the popover. */
  triggerNode: React.ReactElement;
  /** Event that will show/hide the popover. */
  triggerAction?: 'click' | 'hover' | 'focus';
  /**
   * Array of strings that contain items to be displayed as options in the list.
   * You can customize the items appearance by giving an `itemFormatter` prop.
   */
  items?: string[];
  /**
   * Function that will be called for each item in the list. It receives the current item
   * and must return a React node that will be displayed on screen.
   */
  itemFormatter?: (...args: any[]) => React.ReactElement | string;
  /** Indicates whether the component will allow multiple selected items or not. */
  multiple?: boolean;
  /** Indicates which items are selected. This should be the same string that appears in the `items` list. */
  selectedItems?: string[];
  /**
   * Function that will be called when the item selection changes.
   * The function will receive the selected item as first argument or `undefined` if the selection
   * is cleared, and a callback function to hide the popover as a second argument.
   */
  onItemSelect: (...args: any[]) => void;
  /** Indicates whether the component should display a text filter or not. */
  displayDataFilter?: boolean;
  /** Placeholder to display in the filter text input. */
  filterPlaceholder?: string;
  /** Text to display in the entry to clear the current selection. */
  clearSelectionText?: string;
  /** Indicates whether items will be clickable or not. */
  disabled?: boolean;
};

/**
 * Component that displays a list of items in a popover and enable users to pick one of
 * the options with the mouse. The component can (optionally) filter options with a text input
 * and customize how items are displayed with a function.
 */
class SelectPopover extends React.Component<SelectPopoverProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    placement: 'bottom',
    triggerAction: 'click',
    items: [],
    itemFormatter: (item) => item,
    multiple: false,
    selectedItems: [],
    displayDataFilter: true,
    filterPlaceholder: 'Type to filter',
    clearSelectionText: 'Clear selection',
    disabled: false,
  };

  private overlay: { hide: () => void };

  constructor(props) {
    super(props);

    this.state = {
      filterText: '',
      filteredItems: props.items,
      selectedItems: props.selectedItems,
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { items, selectedItems } = this.props;
    const { filterText } = this.state;

    if (!isEqual(selectedItems, nextProps.selectedItems)) {
      this.setState({ selectedItems: nextProps.selectedItems });
    }

    if (items !== nextProps.items) {
      this.filterData(filterText, nextProps.items);
    }
  }

  handleSelectionChange = (nextSelection) => {
    const { onItemSelect } = this.props;

    this.setState({ selectedItems: nextSelection });
    onItemSelect(nextSelection, () => this.overlay.hide());
  };

  clearItemSelection = () => {
    this.handleSelectionChange([]);
  };

  handleItemSelection = (item) => () => {
    const { multiple } = this.props;
    const { selectedItems } = this.state;
    let nextSelectedItems;

    if (multiple) {
      // Clicking on a selected value on a multiselect input will toggle the item's select status
      nextSelectedItems = selectedItems.includes(item) ? without(selectedItems, item) : concat(selectedItems, item);
    } else {
      nextSelectedItems = [item];
    }

    this.handleSelectionChange(nextSelectedItems);
  };

  filterData = (filterText, items) => {
    const newFilteredItems = items.filter((item) => item.match(new RegExp(filterText, 'i')));

    this.setState({ filterText: filterText, filteredItems: newFilteredItems });
  };

  handleFilterChange = (items) => (event) => {
    const filterText = event.target.value.trim();

    this.filterData(filterText, items);
  };

  renderDataFilter = (items) => {
    const { filterPlaceholder } = this.props;
    const { filterText } = this.state;

    return (
      <FormGroup controlId="dataFilterInput" className={style.dataFilterInput}>
        <FormControl type="text"
                     placeholder={filterPlaceholder}
                     value={filterText}
                     onChange={this.handleFilterChange(items)} />
      </FormGroup>
    );
  };

  renderClearSelectionItem = () => {
    const { clearSelectionText } = this.props;

    return (
      <ListGroupItem onClick={this.clearItemSelection}>
        <Icon name="close" className="text-danger" /> {clearSelectionText}
      </ListGroupItem>
    );
  };

  render() {
    const {
      displayDataFilter,
      itemFormatter,
      items,
      placement,
      triggerAction,
      triggerNode,
      disabled,
      title,
    } = this.props;
    const { filteredItems, selectedItems } = this.state;
    const popover = (
      <>
        {displayDataFilter && this.renderDataFilter(items)}
        {selectedItems.length > 0 && this.renderClearSelectionItem()}
        <IsolatedScroll className={style.scrollableList}>
          <ListGroup>
            {filteredItems.map((item) => (
              <ListGroupItem key={item}
                             onClick={disabled ? () => {
                             } : this.handleItemSelection(item)}
                             active={selectedItems.includes(item)}
                             disabled={disabled}>
                {itemFormatter(item)}
              </ListGroupItem>
            ))}
          </ListGroup>
        </IsolatedScroll>
      </>
    );

    return (
      <OverlayTrigger ref={(c) => {
        this.overlay = c;
      }}
                      trigger={triggerAction}
                      placement={placement}
                      overlay={popover}
                      title={title}
                      rootClose>
        {triggerNode}
      </OverlayTrigger>
    );
  }
}

export default SelectPopover;
