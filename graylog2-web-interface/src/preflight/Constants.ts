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
  CERTIFICATE_PROVISIONING: {
    key: 'CERTIFICATE_PROVISIONING',
    description: 'Provision certificates for your data nodes',

  },
  CONFIGURATION_FINISHED: {
    key: 'CONFIGURATION_FINISHED',
    description: 'All data nodes are secured and reachable',
  },
};

export const CONFIGURATION_STEPS_ORDER = [
  CONFIGURATION_STEPS.CA_CONFIGURATION.key,
  CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key,
  CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key,
];

export const DATA_NODES_STATUS = {
  UNCONFIGURED: {
    key: 'UNCONFIGURED',
  },
  CONFIGURED: {
    key: 'UNCONFIGURED',
  },
  CSR: {
    key: 'CSR',
  },
  SIGNED: {
    key: 'SIGNED',
  },
  CONNECTED: {
    key: 'CONNECTED',
  },
  ERROR: {
    key: 'ERROR',
  },
};
