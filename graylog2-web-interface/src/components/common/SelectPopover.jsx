import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { ListGroup, ListGroupItem, OverlayTrigger, Popover } from 'react-bootstrap';

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
  },

  getDefaultProps() {
    return {
      placement: 'bottom',
      triggerAction: 'click',
      items: [],
      itemFormatter: item => item,
      selectedItem: undefined,
      onItemSelect: () => {},
    };
  },

  getInitialState() {
    return {
      selectedItem: this.props.selectedItem,
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.selectedItem !== nextProps.selectedItem) {
      this.setState({ selectedItem: nextProps.selectedItem });
    }
  },

  handleItemSelection(item) {
    return () => {
      this.setState({ selectedItem: item });
      this.props.onItemSelect(item);
    };
  },

  render() {
    const { itemFormatter, items, placement, triggerAction, triggerNode, ...popoverProps } = this.props;
    const popover = (
      <Popover {...popoverProps} className={style.customPopover}>
        <ListGroup>
          {items.map((item) => {
            return (
              <ListGroupItem key={item} onClick={this.handleItemSelection(item)} active={this.state.selectedItem === item}>
                {itemFormatter(item)}
              </ListGroupItem>
            );
          })}
        </ListGroup>
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
