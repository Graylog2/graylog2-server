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
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

const CreatePipelineButton = () => {
  const currentUser = useCurrentUser();

  return (
    <div className="pull-right">
      <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE('new')}>
        <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:create')} bsStyle="success">Add new pipeline</Button>
      </LinkContainer>
    </div>
  );
};

export default CreatePipelineButton;
