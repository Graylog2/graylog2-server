import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { ListGroup } from 'components/graylog';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';

import SortableListItem from './SortableListItem';

const SortableListGroup = styled(ListGroup)(({ disableDragging }) => css`
  cursor: ${disableDragging ? 'default' : 'move'};

  .dragging {
    opacity: 0.5;
  }

  .over {
    border: 1px dashed #8c8e86;
  }

  .list-group-item:first-child {
    border-top-right-radius: 0;
    border-top-left-radius: 0;
  }

  .list-group-item:last-child {
    border-bottom-right-radius: 0;
    border-bottom-left-radius: 0;
  }
`);

/**
 * Component that renders a list of elements and let users manually
 * sort them by dragging and dropping them.
 *
 * `SortableList` keeps the current sorting in its state, so that consumers
 * using a different array or object to keep the sorting state can still
 * use it.
 */
class SortableList extends React.Component {
  static propTypes = {
    /** Specifies if dragging and dropping is disabled or not. */
    disableDragging: PropTypes.bool,
    /**
     * Array of objects that will be displayed in the list. Each item is
     * expected to have an `id` and a `title` key. `id` must be unique
     * and will be used for sorting the item. `title` is used to display the
     * element name in the list.
     */
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    /**
     * Function that will be called when an item of the list was moved.
     * The function will receive the newly sorted list as an argument.
     */
    onMoveItem: PropTypes.func,
  };

  static defaultProps = {
    disableDragging: false,
  };

  state = {
    items: this.props.items,
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ items: nextProps.items });
  }

  _moveItem = (dragIndex, hoverIndex) => {
    const sortedItems = this.state.items;
    const tempItem = sortedItems[dragIndex];
    sortedItems[dragIndex] = sortedItems[hoverIndex];
    sortedItems[hoverIndex] = tempItem;
    this.setState({ items: sortedItems });
    if (typeof this.props.onMoveItem === 'function') {
      this.props.onMoveItem(sortedItems);
    }
  };

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
      <SortableListGroup disableDragging={this.props.disableDragging}>
        {formattedItems}
      </SortableListGroup>
    );
  }
}


export default DragDropContext(HTML5Backend)(SortableList);
