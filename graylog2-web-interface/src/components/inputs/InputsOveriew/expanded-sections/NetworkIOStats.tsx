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

import { Icon } from 'components/common';
import NumberUtils from 'util/NumberUtils';

const NetworkIOStats = ({
  readBytes1Sec,
  writtenBytes1Sec,
  readBytesTotal,
  writtenBytesTotal,
}: {
  readBytes1Sec: number;
  writtenBytes1Sec: number;
  readBytesTotal: number;
  writtenBytesTotal: number;
}) => (
  <span>
    <span>Network IO: </span>
    <span>
      <Icon name="arrow_drop_down" />
      <span>{NumberUtils.formatBytes(readBytes1Sec)} </span>

      <Icon name="arrow_drop_up" />

      <span>{NumberUtils.formatBytes(writtenBytes1Sec)}</span>
    </span>

    <span>
      <span> (total: </span>
      <Icon name="arrow_drop_down" />
      <span>{NumberUtils.formatBytes(readBytesTotal)} </span>

      <Icon name="arrow_drop_up" />
      <span>{NumberUtils.formatBytes(writtenBytesTotal)}</span>
      <span> )</span>
    </span>
  </span>
);

export default NetworkIOStats;
