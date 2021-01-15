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
import classNames from 'classnames';

import { OverlayTrigger, Tooltip } from 'components/graylog';

class ShardRouting extends React.Component {
  static propTypes = {
    route: PropTypes.object.isRequired,
  };

  render() {
    const { route } = this.props;
    const tooltip = <Tooltip id="shard-route-state-tooltip">State: <i>{route.state}</i> on {route.node_hostname} ({route.node_name})</Tooltip>;

    return (
      <li className={classNames('shard', `shard-${route.state}`, { 'shard-primary': route.primary })}>
        <OverlayTrigger placement="top" overlay={tooltip}>
          <span className="id">S{route.id}</span>
        </OverlayTrigger>
      </li>
    );
  }
}

export default ShardRouting;
