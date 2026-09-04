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
import useRoutingQuery from 'routing/useQuery';
import { REPLAY_SESSION_ID_PARAM } from 'components/events/Constants';
import Store from 'logic/local-storage/Store';

const useSessionInitialEventIds = (): Array<string> => {
  const params = useRoutingQuery();
  const replaySessionId = params[REPLAY_SESSION_ID_PARAM];

  return Store.sessionGet<{ initialEventIds?: Array<string> }>(replaySessionId as string)?.initialEventIds ?? [];
};

export default useSessionInitialEventIds;
