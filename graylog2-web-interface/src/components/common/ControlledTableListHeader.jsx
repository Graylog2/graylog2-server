import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { ListGroupItem } from 'react-bootstrap';

const ControlledTableListHeader = createReactClass({
  propTypes: {
    children: PropTypes.node,
  },

  render() {
    const header = (
      <div style={{ height: 40, paddingTop: 10, paddingBottom: 10, margin: 0, fontSize: 14 }}>
        {this.props.children}
      </div>
    );

    return <ListGroupItem className="list-group-header" header={header} />;
  },
});

export default ControlledTableListHeader;
