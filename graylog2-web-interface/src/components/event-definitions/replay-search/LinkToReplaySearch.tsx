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
import useParams from 'routing/useParams';
import usePermissions from 'hooks/usePermissions';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';

type Props = {
  id?: string;
  eventDefinitionId?: string;
  isEvent?: boolean;
  onClick?: () => void;
  isMenuitem?: boolean;
};
const LinkToReplaySearch = ({
  isEvent = false,
  id = undefined,
  eventDefinitionId = undefined,
  onClick = undefined,
  isMenuitem = false,
}: Props) => {
  const { isPermitted } = usePermissions();
  const { definitionId } = useParams<{ alertId?: string; definitionId?: string }>();
  const replaySearchEventDefinitionId = eventDefinitionId || (!isEvent ? id || definitionId : undefined);

  if (replaySearchEventDefinitionId && !isPermitted(`eventdefinitions:read:${replaySearchEventDefinitionId}`)) {
    return null;
  }

  const searchLink = isEvent
    ? Routes.ALERTS.replay_search(id)
    : Routes.ALERTS.DEFINITIONS.replay_search(replaySearchEventDefinitionId);

  return (
    <ReplaySearchButtonComponent searchLink={searchLink} onClick={onClick} component={isMenuitem ? MenuItem : undefined}>
      Replay search
    </ReplaySearchButtonComponent>
  );
};

export default LinkToReplaySearch;
