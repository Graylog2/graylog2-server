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
