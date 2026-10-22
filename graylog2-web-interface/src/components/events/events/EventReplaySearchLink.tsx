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

import Routes from 'routing/Routes';
import { ReplaySearchButtonComponent } from 'views/components/widgets/ReplaySearchButton';
import usePermissions from 'hooks/usePermissions';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';

/**
 * Link to replay the search for a specific event, using that event's own parameters. Hidden unless the user
 * can read the event's event definition, since the replay search itself requires that permission.
 */

type Props = {
  eventId: string;
  eventDefinitionId: string;
  onClick?: () => void;
  isMenuitem?: boolean;
};

const EventReplaySearchLink = ({ eventId, eventDefinitionId, onClick = undefined, isMenuitem = false }: Props) => {
  const { isPermitted } = usePermissions();

  if (!isPermitted(`eventdefinitions:read:${eventDefinitionId}`)) {
    return null;
  }

  return (
    <ReplaySearchButtonComponent
      searchLink={Routes.ALERTS.replay_search(eventId)}
      onClick={onClick}
      component={isMenuitem ? MenuItem : undefined}>
      Replay search
    </ReplaySearchButtonComponent>
  );
};

export default EventReplaySearchLink;
