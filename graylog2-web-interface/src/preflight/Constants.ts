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

export const CONFIGURATION_STEPS = {
  CA_CONFIGURATION: {
    key: 'CA_CONFIGURATION',
    description: 'Configure a certificate authority',
  },
  RENEWAL_POLICY_CONFIGURATION: {
    key: 'RENEWAL_POLICY_CONFIGURATION',
    description: 'Configure a renewal policy',
  },
  CERTIFICATE_PROVISIONING: {
    key: 'CERTIFICATE_PROVISIONING',
    description: 'Provision certificates for your data nodes',
  },
  CONFIGURATION_FINISHED: {
    key: 'CONFIGURATION_FINISHED',
    description: 'Configuration finished',
  },
} as const;

export const CONFIGURATION_STEPS_ORDER = [
  CONFIGURATION_STEPS.CA_CONFIGURATION.key,
  CONFIGURATION_STEPS.RENEWAL_POLICY_CONFIGURATION.key,
  CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key,
  CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key,
];

export const DATA_NODES_STATUS = {
  UNCONFIGURED: 'UNCONFIGURED',
  CONNECTED: 'CONNECTED',
  ERROR: 'ERROR',
  STARTING: 'STARTING',
  PROVISIONED: 'PROVISIONED',
} as const;
