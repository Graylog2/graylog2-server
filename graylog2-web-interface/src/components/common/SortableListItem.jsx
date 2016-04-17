import React, {PropTypes} from 'react';
import ReactDOM from 'react-dom';
import {ListGroupItem} from 'react-bootstrap';
import { DragSource, DropTarget } from 'react-dnd';

const ItemTypes = {
  ITEM: 'item',
};

const itemSource = {
  beginDrag(props) {
    return {
      id: props.id,
      index: props.index,
    };
  },
};

const itemTarget = {
  hover(props, monitor, component) {
    const dragIndex = monitor.getItem().index;
    const hoverIndex = props.index;

    // Don't replace items with themselves
    if (dragIndex === hoverIndex) {
      return;
    }

    // Determine rectangle on screen
    const hoverBoundingRect = ReactDOM.findDOMNode(component).getBoundingClientRect();

    // Get vertical middle
    const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

    // Determine mouse position
    const clientOffset = monitor.getClientOffset();

    // Get pixels to the top
    const hoverClientY = clientOffset.y - hoverBoundingRect.top;

    // Only perform the move when the mouse has crossed half of the items height
    // When dragging downwards, only move when the cursor is below 50%
    // When dragging upwards, only move when the cursor is above 50%

    // Dragging downwards
    if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
      return;
    }

    // Dragging upwards
    if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
      return;
    }

    // Time to actually perform the action
    props.moveItem(dragIndex, hoverIndex);

    // Note: we're mutating the monitor item here!
    // Generally it's better to avoid mutations,
    // but it's good here for the sake of performance
    // to avoid expensive index searches.
    monitor.getItem().index = hoverIndex;
  },
};

function collectSource(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging(),
  };
}

function collectTarget(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver(),
  };
}

const SortableListItem = React.createClass({
  propTypes: {
    connectDragSource: PropTypes.func.isRequired,
    connectDropTarget: PropTypes.func.isRequired,
    index: PropTypes.number.isRequired,
    isDragging: PropTypes.bool.isRequired,
    isOver: PropTypes.bool.isRequired,
    id: PropTypes.any.isRequired,
    text: PropTypes.string.isRequired,
    moveItem: PropTypes.func.isRequired,
  },
  render() {
    const { text, isDragging, isOver, connectDragSource, connectDropTarget } = this.props;
    const classes = [];
    if (isDragging) {
      classes.push('dragging');
    }
    if (isOver) {
      classes.push('over');
    }

    return connectDragSource(connectDropTarget(
      <div className="sortable-list-item">
        <ListGroupItem className={classes.join(' ')}>
          <i className="fa fa-sort" style={{marginRight: 10}}/> {text}
        </ListGroupItem>
      </div>
    ));
  },
});

export default DropTarget(ItemTypes.ITEM, itemTarget, collectTarget)(DragSource(ItemTypes.ITEM, itemSource, collectSource)(SortableListItem));
