import React, { PropTypes } from 'react';
import { Timestamp } from 'components/common';

const RestApiOverview = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
  },
  render() {
    return (
      <dl className="system-rest">
        <dt>Transport address:</dt>
        <dd>{this.props.node.transport_address}</dd>
        <dt>Last seen:</dt>
        <dd><Timestamp dateTime={this.props.node.last_seen} relative /></dd>
      </dl>
    );
  },
});

export default RestApiOverview;
