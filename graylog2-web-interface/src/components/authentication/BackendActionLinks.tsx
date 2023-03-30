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

import { LinkContainer } from 'components/common/router';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/bootstrap';

type Props = {
  activeBackend: AuthenticationBackend | undefined,
  finishedLoading: boolean,
};

const BackendActionLinks = ({ activeBackend, finishedLoading }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE}>
      <Button disabled={!finishedLoading || !activeBackend}>
        View active service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(activeBackend?.id)}>
      <Button disabled={!activeBackend || !finishedLoading}
              type="button">
        Edit active service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE}>
      <Button bsStyle="success" type="button">
        Create service
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default BackendActionLinks;
