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

import Routes from 'routing/Routes';
import SubareaNavigation from 'components/common/SubareaNavigation';
import { Row } from 'components/bootstrap';

const UsersSubareaNavigation = () => {
  const NAV_ITEMS = [
    { title: 'Users Overview', path: Routes.SYSTEM.USERS.OVERVIEW, permissions: 'users:list' },
    { title: 'Teams Overview', path: Routes.getPluginRoute('SYSTEM_TEAMS'), permissions: 'teams:list' },
  ];

  return (
    <Row>
      <SubareaNavigation items={NAV_ITEMS} />
    </Row>
  );
};

export default UsersSubareaNavigation;
