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

const LinkToReplaySearch = ({ isEvent, id }: { id?: string, isEvent?: boolean }) => {
  const { definitionId } = useParams<{ alertId?: string, definitionId?: string }>();
  const searchLink = isEvent ? Routes.ALERTS.replay_search(id) : Routes.ALERTS.DEFINITIONS.replay_search(id || definitionId);

  return (
    <ReplaySearchButtonComponent searchLink={searchLink}>Replay search</ReplaySearchButtonComponent>
  );
};

LinkToReplaySearch.defaultProps = {
  id: undefined,
  isEvent: false,
};

export default LinkToReplaySearch;
