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
    /** Indicates which is the selected item. This should be the same string that appears in the `items` list. */
    selectedItem: PropTypes.string,
    /**
     * Function that will be called when the item selection changes.
     * The function will receive the selected item as argument or `undefined` if the selection
     * is cleared.
     */
    onItemSelect: PropTypes.func.isRequired,
    /** Indicates whether the component should display a text filter or not. */
    displayDataFilter: PropTypes.bool,
    /** Placeholder to display in the filter text input. */
    filterPlaceholder: PropTypes.string,
  },

  getDefaultProps() {
    return {
      placement: 'bottom',
      triggerAction: 'click',
      items: [],
      itemFormatter: item => item,
      selectedItem: undefined,
      onItemSelect: () => {},
      displayDataFilter: true,
      filterPlaceholder: 'Type to filter',
    };
  },

  getInitialState() {
    return {
      filterText: '',
      filteredItems: this.props.items,
      selectedItem: this.props.selectedItem,
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.selectedItem !== nextProps.selectedItem) {
      this.setState({ selectedItem: nextProps.selectedItem });
    }
    if (this.props.items !== nextProps.items) {
      this.filterData(this.state.filterText, nextProps.items);
    }
  },

  handleItemSelection(item) {
    return () => {
      this.setState({ selectedItem: item });
      this.props.onItemSelect(item);
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
      <ListGroupItem onClick={this.handleItemSelection()}><i className="fa fa-fw fa-times text-danger" /> Clear selection</ListGroupItem>
    );
  },

  render() {
    const { displayDataFilter, itemFormatter, items, placement, triggerAction, triggerNode, ...otherProps } = this.props;
    const popoverProps = this.pickPopoverProps(otherProps);
    const { filteredItems, selectedItem } = this.state;

    const popover = (
      <Popover {...popoverProps} className={style.customPopover}>
        {displayDataFilter && this.renderDataFilter(items)}
        {selectedItem && this.renderClearSelectionItem()}
        <IsolatedScroll className={style.scrollableList}>
          <ListGroup>
            {filteredItems.map((item) => {
              return (
                <ListGroupItem key={item}
                               onClick={this.handleItemSelection(item)}
                               active={this.state.selectedItem === item}>
                  {itemFormatter(item)}
                </ListGroupItem>
              );
            })}
          </ListGroup>
        </IsolatedScroll>
      </Popover>
    );

    return (
      <OverlayTrigger trigger={triggerAction} placement={placement} overlay={popover} rootClose>
        {triggerNode}
      </OverlayTrigger>
    );
  },
});

export default SelectPopover;
