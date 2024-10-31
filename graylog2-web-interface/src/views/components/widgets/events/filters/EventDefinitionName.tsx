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

import { Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useEventDefinition from 'components/events/events/hooks/useEventDefinition';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

type Props = {
  eventDefinitionId: string,
  displayAsLink?: boolean,
}

const EventDefinitionName = ({ eventDefinitionId, displayAsLink = true }: Props) => {
  const currentUser = useCurrentUser();
  const canViewDefinition = isPermitted(currentUser.permissions, `eventdefinitions:read:${eventDefinitionId}`);
  const { data: eventDefinition, isFetching } = useEventDefinition(eventDefinitionId, canViewDefinition);
  const title = eventDefinition?.title ?? eventDefinitionId;

  if (isFetching) {
    return <Spinner />;
  }

  if (!displayAsLink || !canViewDefinition) {
    return <>{title}</>;
  }

  if (eventDefinition) {
    return (
      <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)} target="_blank">
        {title}
      </Link>
    );
  }

  return null;
};

export default EventDefinitionName;
