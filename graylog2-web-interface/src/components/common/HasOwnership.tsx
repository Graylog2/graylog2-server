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

import useHasEntityOwnership from 'hooks/useHasEntityOwnership';

type ChildFun = (props: { disabled: boolean }) => React.ReactElement;

type Props = {
  children: ChildFun;
  id?: string;
  type: string;
  hideChildren?: boolean;
};

const HasOwnership = ({ children, id = undefined, type, hideChildren = false }: Props) => {
  const hasOwnership = useHasEntityOwnership(id, type);

  if (typeof children !== 'function') {
    throw new Error('Invalid prop: "children" must be a function.');
  }

  if (hideChildren) {
    return null;
  }

  return <>{children({ disabled: !hasOwnership })} </>;
};

export default HasOwnership;
