import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { FormControl, FormGroup, ListGroup, ListGroupItem, OverlayTrigger, Popover } from 'react-bootstrap';
import lodash from 'lodash';
import IsolatedScroll from 'react-isolated-scroll';

import style from './SelectPopover.css';

const SelectPopover = createReactClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
    title: PropTypes.string.isRequired,
    triggerNode: PropTypes.node.isRequired,
    triggerAction: PropTypes.oneOf(['click', 'hover', 'focus']),
    items: PropTypes.arrayOf(PropTypes.string),
    itemFormatter: PropTypes.func,
    selectedItem: PropTypes.string,
    onItemSelect: PropTypes.func.isRequired,
    displayDataFilter: PropTypes.bool,
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
