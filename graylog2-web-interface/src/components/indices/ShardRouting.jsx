import React from 'react';
import classNames from 'classnames';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';

const ShardRouting = React.createClass({
  propTypes: {
    route: React.PropTypes.object.isRequired,
  },
  render() {
    const route = this.props.route;
    const tooltip = <Tooltip id="shard-route-state-tooltip">State: <i>{route.state}</i> on {route.node_hostname} ({route.node_name})</Tooltip>;
    return (
      <li className={classNames('shard', `shard-${route.state}`, { 'shard-primary': route.primary })}>
        <OverlayTrigger placement="top" overlay={tooltip}>
          <span className="id">S{route.id}</span>
        </OverlayTrigger>
      </li>
    );
  },
});

export default ShardRouting;
