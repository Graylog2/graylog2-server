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

import { Alert } from 'components/graylog';
import { Icon } from 'components/common';

const ProfileUpdateInfo = () => (
  <Alert bsStyle="info">
    <Icon name="info-circle" />{' '}<b> First and Last Name</b><br />
    With Graylog 4.1, we&apos;ve added distinct first and last name fields. These must be provided before the user’s profile can be saved.
  </Alert>
);

export default ProfileUpdateInfo;
