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
const Routes = {
  INTEGRATIONS: {
    AWS: {
      CLOUDWATCH: {
        index: '/integrations/aws/cloudwatch',
      },
    },
  },
};

const ApiRoutes = {
  INTEGRATIONS: {
    AWS: {
      PERMISSIONS: '/plugins/org.graylog.integrations/aws/permissions',
      REGIONS: '/plugins/org.graylog.integrations/aws/regions',
      CLOUDWATCH: {
        GROUPS: '/plugins/org.graylog.integrations/aws/cloudwatch/log_groups',
      },
      KINESIS: {
        HEALTH_CHECK: '/plugins/org.graylog.integrations/aws/kinesis/health_check',
        STREAMS: '/plugins/org.graylog.integrations/aws/kinesis/streams',
        SAVE: '/plugins/org.graylog.integrations/aws/inputs',
      },
      KINESIS_AUTO_SETUP: {
        CREATE_STREAM: '/plugins/org.graylog.integrations/aws/kinesis/auto_setup/create_stream',
        CREATE_SUBSCRIPTION_POLICY: '/plugins/org.graylog.integrations/aws/kinesis/auto_setup/create_subscription_policy',
        CREATE_SUBSCRIPTION: '/plugins/org.graylog.integrations/aws/kinesis/auto_setup/create_subscription',
      },
    },
  },
};

const DocsRoutes = {
  INTEGRATIONS: {
    AWS: {
      AWS_KINESIS_CLOUDWATCH_INPUTS: 'integrations/inputs/aws_kinesis_cloudwatch_input.html#aws-kinesis-cloudwatch-input',
    },
  },
};

export default Routes;

export { ApiRoutes, DocsRoutes };
