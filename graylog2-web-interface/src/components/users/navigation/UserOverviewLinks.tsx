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

import { LinkContainer } from 'components/graylog/router';
import { IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const UserOverviewLinks = () => {
  const teamsRoute = Routes.getPluginRoute('SYSTEM_TEAMS');

  return (
    <ButtonToolbar>
      <IfPermitted permissions="users:list">
        <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
          <Button bsStyle="info">
            Users Overview
          </Button>
        </LinkContainer>
      </IfPermitted>
      {teamsRoute && (
        <IfPermitted permissions="teams:list">
          <LinkContainer to={teamsRoute}>
            <Button bsStyle="info">Teams Overview</Button>
          </LinkContainer>
        </IfPermitted>
      )}
    </ButtonToolbar>
  );
};

export default UserOverviewLinks;
