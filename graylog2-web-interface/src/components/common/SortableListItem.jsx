import React from 'react';
import ReactDOM from 'react-dom';
import { ListGroupItem } from 'react-bootstrap';
import { DragSource, DropTarget } from 'react-dnd';

import SortableListItemStyle from '!style!css!components/common/SortableListItem.css';

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
    connectDragSource: React.PropTypes.func.isRequired,
    connectDropTarget: React.PropTypes.func.isRequired,
    content: React.PropTypes.any.isRequired,
    disableDragging: React.PropTypes.bool,
    index: React.PropTypes.number.isRequired,
    isDragging: React.PropTypes.bool.isRequired,
    isOver: React.PropTypes.bool.isRequired,
    id: React.PropTypes.any.isRequired,
    moveItem: React.PropTypes.func.isRequired,
  },
  getDefaultProps() {
    return {
      disableDragging: false,
    };
  },

  render() {
    const { content, isDragging, isOver, connectDragSource, connectDropTarget } = this.props;
    const classes = [SortableListItemStyle.listGroupItem];
    if (isDragging) {
      classes.push('dragging');
    }
    if (isOver) {
      classes.push('over');
    }

    const handle = <span className={SortableListItemStyle.itemHandle}><i className="fa fa-sort" /></span>;

    const component = (
      <div className="sortable-list-item">
        <ListGroupItem className={classes.join(' ')}>
          <div>
            {this.props.disableDragging ? null : handle}
            {content}
          </div>
        </ListGroupItem>
      </div>
    );

    return this.props.disableDragging ? component : connectDragSource(connectDropTarget(component));
  },
});

export default DropTarget(ItemTypes.ITEM, itemTarget, collectTarget)(DragSource(ItemTypes.ITEM, itemSource, collectSource)(SortableListItem));
