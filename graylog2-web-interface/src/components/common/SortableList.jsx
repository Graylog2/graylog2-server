import React, { PropTypes } from 'react';
import { ListGroup } from 'react-bootstrap';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';

import SortableListItem from './SortableListItem';

const SortableList = React.createClass({
  propTypes: {
    disableDragging: React.PropTypes.bool,
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    onMoveItem: PropTypes.func,
  },
  getDefaultProps() {
    return {
      disableDragging: false,
    };
  },

  getInitialState() {
    return {
      items: this.props.items,
    };
  },
  componentWillReceiveProps(nextProps) {
    this.setState({ items: nextProps.items });
  },
  _moveItem(dragIndex, hoverIndex) {
    const sortedItems = this.state.items;
    const tempItem = sortedItems[dragIndex];
    sortedItems[dragIndex] = sortedItems[hoverIndex];
    sortedItems[hoverIndex] = tempItem;
    this.setState({ items: sortedItems });
    if (typeof this.props.onMoveItem === 'function') {
      this.props.onMoveItem(sortedItems);
    }
  },
  render() {
    const formattedItems = this.state.items.map((item, idx) => {
      return (
        <SortableListItem key={`sortable-list-item-${item.id}`}
                          disableDragging={this.props.disableDragging}
                          index={idx}
                          id={item.id}
                          content={item.title}
                          moveItem={this._moveItem} />
      );
    });

    return (
      <ListGroup className={this.props.disableDragging ? 'sortable-list' : 'sortable-list sortable-list-cursor'}>
        {formattedItems}
      </ListGroup>
    );
  },
});


export default DragDropContext(HTML5Backend)(SortableList);
