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
import usePermissions from 'hooks/usePermissions';

export const ALERTS_EVENTS_STREAMS = ['000000000000000000000003', '000000000000000000000002'];

const usePermittedAlertsEventsStreams = () => {
  const { isPermitted } = usePermissions();

  return ALERTS_EVENTS_STREAMS.filter((streamId) => isPermitted(`streams:read:${streamId}`));
};

export default usePermittedAlertsEventsStreams;
