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

import QueryHelper from 'components/common/QueryHelper';

const queryExample = (
  <p>
    Find users with a email containing example.com:<br />
    <kbd>email:example.com</kbd><br />
  </p>
);

const fieldMap = {
  full_name: 'The full name of a user',
  username: 'The users login username.',
  email: 'The users email.',
};

const UserQueryHelper = () => (
  <QueryHelper example={queryExample} commonFields={[]} fieldMap={fieldMap} entityName="user" />
);

export default UserQueryHelper;
