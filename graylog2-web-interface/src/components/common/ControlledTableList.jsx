import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { ListGroup } from 'react-bootstrap';

import ControlledTableListHeader from './ControlledTableListHeader';
import ControlledTableListItem from './ControlledTableListItem';

const ControlledTableList = createReactClass({
  propTypes: {
    children: PropTypes.node.isRequired,
  },

  render() {
    const { children } = this.props;
    let effectiveChildren;

    if (children.length === 0) {
      effectiveChildren = <ControlledTableListItem>No items to display</ControlledTableListItem>;
    } else {
      effectiveChildren = children;
    }

    return (
      <div>
        <ListGroup>
          {effectiveChildren}
        </ListGroup>
      </div>
    );
  },
});

ControlledTableList.Header = ControlledTableListHeader;
ControlledTableList.Item = ControlledTableListItem;

export default ControlledTableList;
