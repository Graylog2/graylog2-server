import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { ListGroup } from 'components/graylog';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';

import SortableListItem from './SortableListItem';

const SortableListGroup = styled(ListGroup)(({ disableDragging, theme }) => css`
  cursor: ${disableDragging ? 'default' : 'move'};
  margin: 0 0 15px;

  .dragging {
    opacity: 0.5;
  }

  .over {
    border: 1px dashed ${theme.color.gray[50]};
  }

  .list-group-item {
    border-radius: 0;
  }

  & > div:first-child .list-group-item {
    border-top-right-radius: 4px;
    border-top-left-radius: 4px;
  }

  & > div:last-child .list-group-item {
    border-bottom-right-radius: 4px;
    border-bottom-left-radius: 4px;
    margin-bottom: 0;
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
    onMoveItem: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      items: props.items,
    };
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ items: nextProps.items });
  }

  _moveItem = (dragIndex, hoverIndex) => {
    const { onMoveItem } = this.props;
    const { items } = this.state;
    const tempItem = items[dragIndex];
    items[dragIndex] = items[hoverIndex];
    items[hoverIndex] = tempItem;
    this.setState({ items });

    if (typeof onMoveItem === 'function') {
      onMoveItem(items);
    }
  };

  render() {
    const { items } = this.state;
    const { disableDragging } = this.props;

    const formattedItems = items.map((item, idx) => {
      return (
        <SortableListItem key={`sortable-list-item-${item.id}`}
                          disableDragging={disableDragging}
                          index={idx}
                          id={item.id}
                          content={item.title}
                          moveItem={this._moveItem} />
      );
    });

    return (
      <SortableListGroup disableDragging={disableDragging}>
        {formattedItems}
      </SortableListGroup>
    );
  }
}


export default DragDropContext(HTML5Backend)(SortableList);
