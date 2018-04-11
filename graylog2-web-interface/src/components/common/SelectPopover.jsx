import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { FormControl, FormGroup, ListGroup, ListGroupItem, OverlayTrigger, Popover } from 'react-bootstrap';
import lodash from 'lodash';
import IsolatedScroll from 'react-isolated-scroll';

import style from './SelectPopover.css';

/**
 * Component that displays a list of items in a popover and enable users to pick one of
 * the options with the mouse. The component can (optionally) filter options with a text input
 * and customize how items are displayed with a function.
 */
const SelectPopover = createReactClass({
  propTypes: {
    /** Provides an ID for this popover element. */
    id: PropTypes.string.isRequired,
    /** Indicates where the popover should appear. */
    placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
    /** Title to use in the popover header. */
    title: PropTypes.string.isRequired,
    /** React node that will be used as trigger to show/hide the popover. */
    triggerNode: PropTypes.node.isRequired,
    /** Event that will show/hide the popover. */
    triggerAction: PropTypes.oneOf(['click', 'hover', 'focus']),
    /**
     * Array of strings that contain items to be displayed as options in the list.
     * You can customize the items appearance by giving an `itemFormatter` prop.
     */
    items: PropTypes.arrayOf(PropTypes.string),
    /**
     * Function that will be called for each item in the list. It receives the current item
     * and must return a React node that will be displayed on screen.
     */
    itemFormatter: PropTypes.func,
    /** Indicates whether the component will allow multiple selected items or not. */
    multiple: PropTypes.bool,
    /** Indicates which items are selected. This should be the same string that appears in the `items` list. */
    selectedItems: PropTypes.arrayOf(PropTypes.string),
    /**
     * Function that will be called when the item selection changes.
     * The function will receive the selected item as first argument or `undefined` if the selection
     * is cleared, and a callback function to hide the popover as a second argument.
     */
    onItemSelect: PropTypes.func.isRequired,
    /** Indicates whether the component should display a text filter or not. */
    displayDataFilter: PropTypes.bool,
    /** Placeholder to display in the filter text input. */
    filterPlaceholder: PropTypes.string,
    /** Text to display in the entry to clear the current selection. */
    clearSelectionText: PropTypes.string,
    /** Indicates whether items will be clickable or not. */
    disabled: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      placement: 'bottom',
      triggerAction: 'click',
      items: [],
      itemFormatter: item => item,
      multiple: false,
      selectedItems: [],
      onItemSelect: () => {},
      displayDataFilter: true,
      filterPlaceholder: 'Type to filter',
      clearSelectionText: 'Clear selection',
      disabled: false,
    };
  },

  getInitialState() {
    return {
      filterText: '',
      filteredItems: this.props.items,
      selectedItems: this.props.selectedItems,
    };
  },

  componentWillReceiveProps(nextProps) {
    if (!lodash.isEqual(this.props.selectedItems, nextProps.selectedItems)) {
      this.setState({ selectedItems: nextProps.selectedItems });
    }
    if (this.props.items !== nextProps.items) {
      this.filterData(this.state.filterText, nextProps.items);
    }
  },

  handleSelectionChange(nextSelection) {
    this.setState({ selectedItems: nextSelection });
    this.props.onItemSelect(nextSelection, () => this.overlay.hide());
  },

  clearItemSelection() {
    this.handleSelectionChange([]);
  },

  handleItemSelection(item) {
    return () => {
      const selectedItems = this.state.selectedItems;
      let nextSelectedItems;
      if (this.props.multiple) {
        // Clicking on a selected value on a multiselect input will toggle the item's select status
        nextSelectedItems = selectedItems.includes(item) ? lodash.without(selectedItems, item) : lodash.concat(selectedItems, item);
      } else {
        nextSelectedItems = [item];
      }
      this.handleSelectionChange(nextSelectedItems);
    };
  },

  filterData(filterText, items) {
    const newFilteredItems = items.filter(item => item.match(new RegExp(filterText, 'i')));
    this.setState({ filterText: filterText, filteredItems: newFilteredItems });
  },

  handleFilterChange(items) {
    return (event) => {
      const filterText = event.target.value.trim();
      this.filterData(filterText, items);
    };
  },

  pickPopoverProps(props) {
    const popoverPropKeys = Object.keys(Popover.propTypes);
    return lodash.pick(props, popoverPropKeys);
  },

  renderDataFilter(items) {
    return (
      <FormGroup controlId="dataFilterInput" className={style.dataFilterInput}>
        <FormControl type="text" placeholder={this.props.filterPlaceholder} onChange={this.handleFilterChange(items)} />
      </FormGroup>
    );
  },

  renderClearSelectionItem() {
    return (
      <ListGroupItem onClick={this.clearItemSelection}>
        <i className="fa fa-fw fa-times text-danger" /> {this.props.clearSelectionText}
      </ListGroupItem>
    );
  },

  render() {
    const { displayDataFilter, itemFormatter, items, placement, triggerAction, triggerNode, disabled, ...otherProps } = this.props;
    const popoverProps = this.pickPopoverProps(otherProps);
    const { filteredItems, selectedItems } = this.state;

    const popover = (
      <Popover {...popoverProps} className={style.customPopover}>
        {displayDataFilter && this.renderDataFilter(items)}
        {selectedItems.length > 0 && this.renderClearSelectionItem()}
        <IsolatedScroll className={style.scrollableList}>
          <ListGroup>
            {filteredItems.map((item) => {
              return (
                <ListGroupItem key={item}
                               onClick={disabled ? () => {} : this.handleItemSelection(item)}
                               active={this.state.selectedItems.includes(item)}
                               disabled={disabled}>
                  {itemFormatter(item)}
                </ListGroupItem>
              );
            })}
          </ListGroup>
        </IsolatedScroll>
      </Popover>
    );

    return (
      <OverlayTrigger ref={(c) => { this.overlay = c; }}
                      trigger={triggerAction}
                      placement={placement}
                      overlay={popover}
                      rootClose>
        {triggerNode}
      </OverlayTrigger>
    );
  },
});

export default SelectPopover;
