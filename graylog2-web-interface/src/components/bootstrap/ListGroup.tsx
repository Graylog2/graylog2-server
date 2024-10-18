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
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { ListGroup as BootstrapListGroup } from 'react-bootstrap';

type Props = React.PropsWithChildren<{
  className?: string,
  componentClass?: React.ElementType | undefined,
  bsClass?: React.ComponentProps<typeof BootstrapListGroup>['bsClass'],
  style?: React.ComponentProps<typeof BootstrapListGroup>['style'],
}>

const ListGroup = ({ className, children, ...props }: Props) => <BootstrapListGroup bsClass={className} {...props}>{children}</BootstrapListGroup>;

/** @component */
export default ListGroup;
