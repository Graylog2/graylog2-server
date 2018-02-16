import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { ListGroupItem } from 'react-bootstrap';

const ControlledTableListItem = createReactClass({
  propTypes: {
    children: PropTypes.node.isRequired,
  },

  render() {
    return (
      <ListGroupItem>
        {this.props.children}
      </ListGroupItem>
    );
  },
});

export default ControlledTableListItem;
