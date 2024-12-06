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

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const EventDefinitionLink = ({ title, id }: { title: string | undefined, id: string }) => {
  const { permissions } = useCurrentUser();

  if (!title) {
    return <em>{id}</em>;
  }

  if (isPermitted(permissions, `eventdefinitions:edit:${id}`)) {
    return (
      <Link to={Routes.ALERTS.DEFINITIONS.edit(id)}>
        {title}
      </Link>
    );
  }

  return <>{title}</>;
};

export default EventDefinitionLink;
