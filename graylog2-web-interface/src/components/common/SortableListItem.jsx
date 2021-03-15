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
import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { DragSource, DropTarget } from 'react-dnd';
import styled from 'styled-components';

import { ListGroupItem } from 'components/graylog';

import Icon from './Icon';

// eslint-disable-next-line import/no-webpack-loader-syntax
import SortableListItemStyle from '!style!css!components/common/SortableListItem.css';

const ContentContainer = styled.div`
  display: flex;
`;

const Handle = styled.div`
  margin-right: 5px;
`;

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
    // eslint-disable-next-line react/no-find-dom-node
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
    // eslint-disable-next-line no-param-reassign
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

/**
 * Component that renders an item entry in a `SortableList` component.
 * You most likely don't want to use this component directly, so please
 * check the `SortableList` documentation instead.
 */
class SortableListItem extends React.Component {
  static propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    connectDropTarget: PropTypes.func.isRequired,
    content: PropTypes.any.isRequired,
    disableDragging: PropTypes.bool,
    // Is used via DnD libray
    // eslint-disable-next-line react/no-unused-prop-types
    index: PropTypes.number.isRequired,
    isDragging: PropTypes.bool.isRequired,
    isOver: PropTypes.bool.isRequired,
    // Is used via DnD libray
    // eslint-disable-next-line react/no-unused-prop-types
    id: PropTypes.any.isRequired,
    // Is used via DnD libray
    // eslint-disable-next-line react/no-unused-prop-types
    moveItem: PropTypes.func.isRequired,
    customContentRender: PropTypes.func,
  };

  static defaultProps = {
    disableDragging: false,
    customContentRender: undefined,
  };

  render() {
    const { content, isDragging, isOver, connectDragSource, connectDropTarget, disableDragging, customContentRender } = this.props;
    const classes = [SortableListItemStyle.listGroupItem];

    if (isDragging) {
      classes.push('dragging');
    }

    if (isOver) {
      classes.push('over');
    }

    const finalContent = customContentRender ? customContentRender(content) : content;

    const handle = <Handle className={SortableListItemStyle.itemHandle}><Icon name="sort" /></Handle>;

    const component = (
      <div>
        <div>
          <ListGroupItem className={classes.join(' ')}>
            <ContentContainer>
              {disableDragging ? null : handle}
              {finalContent}
            </ContentContainer>
          </ListGroupItem>
        </div>
      </div>
    );

    return disableDragging ? component : connectDragSource(connectDropTarget(component));
  }
}

export default DropTarget(ItemTypes.ITEM, itemTarget, collectTarget)(DragSource(ItemTypes.ITEM, itemSource, collectSource)(SortableListItem));
