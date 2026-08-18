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

import useHasAccessToAnyStream from 'hooks/useHasAccessToAnyStream';
import UserHasNoStreamAccess from 'pages/UserHasNoStreamAccess';

type Props = {
  children: React.ReactElement;
  skipNoStreamsCheck?: boolean;
};

export default ({ children, skipNoStreamsCheck = false }: Props) => {
  const hasAccessToAnyStream = useHasAccessToAnyStream();

  if (skipNoStreamsCheck) {
    return children;
  }

  return hasAccessToAnyStream ? children : <UserHasNoStreamAccess />;
};
