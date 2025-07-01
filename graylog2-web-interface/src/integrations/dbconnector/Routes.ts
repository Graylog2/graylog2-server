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
import { qualifyUrls } from 'routing/Routes';

const DBConnectorRoutes = {
  INTEGRATIONS: {
    DBConnector: {
      ACTIVITYAPI: {
        index: '/integrations/dbconnector',
      },
    },
  },
};

const ApiRoutes = {
  INTEGRATIONS: {
    DBConnector: {
      TEST_INPUT: '/plugins/org.graylog.integrations/dbconnector/testInput',
      SAVE_INPUT: '/plugins/org.graylog.integrations/dbconnector/inputs',
      TIMEZONES: '/plugins/org.graylog.integrations/dbconnector/timezones',
    },
  },
};

const DocsRoutes = {
  INTEGRATIONS: {
    DBConnector: {
      GRAYLOG_DBCONNECTOR_ACTIVITY_LOG_INPUT: 'integrations/inputs/graylog_DBConnector_ActivityLog_Input.html#graylog-dbConnector-activitylog-input',
    },
  },
};

export default {
  ...qualifyUrls(DBConnectorRoutes),
  unqualified: DBConnectorRoutes,
};

export { DocsRoutes, ApiRoutes };
