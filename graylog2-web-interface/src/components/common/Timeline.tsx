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
import { Timeline as MantineTimeline } from '@mantine/core';

type Props = React.PropsWithChildren<{
  active: number,
  bulletSize: number,
  className?: string,
  color: string,
}>

const Timeline = ({ children, bulletSize, color, active, className }: Props) => (
  <MantineTimeline bulletSize={bulletSize}
                   color={color}
                   className={className}
                   active={active}>{children}
  </MantineTimeline>
);

Timeline.Item = MantineTimeline.Item;

export default Timeline;
