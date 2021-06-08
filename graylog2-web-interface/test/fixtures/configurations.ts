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

/* eslint-disable import/prefer-default-export */
export const configuration = {
  'org.graylog2.messageprocessors.MessageProcessorsConfig': {
    processor_order: [
      {
        name: 'AWS Instance Name Lookup',
        class_name: 'aws.classname',
      },
    ],
    disabled_processors: [],
  },
  'org.graylog.plugins.sidecar.system.SidecarConfiguration': {
    sidecar_configuration_override: false,
    sidecar_expiration_threshold: 'P14D',
    sidecar_inactive_threshold: 'PT1M',
    sidecar_send_status: true,
    sidecar_update_interval: 'PT30S',
  },
};
