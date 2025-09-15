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

import { Row } from 'components/lookup-tables/layout-componets';

import Drawer from './Drawer';

type Props = React.ComponentProps<typeof Drawer> & {
  double?: boolean;
};

const DrawerDouble = ({ double = false, ...props }: Props) => (
  <Drawer
    opened={props.opened}
    onClose={props.onClose}
    position="right"
    size={double ? 1260 : props.size}
    overlayProps={{ zIndex: '1030' }}
    title={props.title}>
    <Row $align="stretch">{props.children}</Row>
  </Drawer>
);

export default DrawerDouble;
