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
import * as React from 'react';
import classNames from 'classnames';

import { OverlayTrigger } from 'components/common';

type Props = {
  route: {
    id: string,
    state: string,
    node_hostname: string,
    node_name: string,
    primary: boolean,
  }
}

const ShardRouting = ({ route }: Props) => {
  const tooltip = <>State: <i>{route.state}</i> on {route.node_hostname} ({route.node_name})</>;

  return (
    <li className={classNames('shard', `shard-${route.state}`, { 'shard-primary': route.primary })}>
      <OverlayTrigger placement="top" overlay={tooltip}>
        <span className="id">S{route.id}</span>
      </OverlayTrigger>
    </li>
  );
};

export default ShardRouting;
