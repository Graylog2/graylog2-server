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

const AWSCloudTrailRoutes = {
  INTEGRATIONS: {
    AWSCloudTrail: {
      ACTIVITYAPI: {
        index: '/integrations/cloudtrail',
      },
    },
  },
};

const ApiRoutes = {
  INTEGRATIONS: {
    AWSCloudTrail: {
      SAVE_INPUT: '/plugins/org.graylog.integrations/cloudtrail/inputs?setup_wizard=true',
      CHECK_CREDENTIALS: '/plugins/org.graylog.integrations/cloudtrail/check_credentials',
      GET_AWS_REGIONS: '/plugins/org.graylog.integrations/cloudtrail/getawsregions',
    },
  },
};

const DocsRoutes = {
  INTEGRATIONS: {
    AWSCloudTrail: {
      GRAYLOG_AWSCloudTrail_ACTIVITY_LOG_INPUT:
        'integrations/inputs/graylog_AWSCloudTrail_ActivityLog_Input.html#graylog-AWSCloudTrail-activitylog-input',
    },
  },
};

export default {
  ...qualifyUrls(AWSCloudTrailRoutes),
  unqualified: AWSCloudTrailRoutes,
};

export { DocsRoutes, ApiRoutes };
