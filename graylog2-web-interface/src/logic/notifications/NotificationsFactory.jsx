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

import Routes from 'routing/Routes';

class NotificationsFactory {
  static getValuesForNotification(notification) {
    switch (notification.type) {
      case 'legacy_ldap_config_migration': {
        const { auth_service_id: authServiceId } = notification.details;

        return {
          values: {
            AUTHENTICATION_BACKEND: Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(authServiceId),
          },
        };
      }

      case 'no_input_running':
      case 'input_failure_shutdown':
      case 'input_failed':
      case 'input_failed_to_start': { // eslint-disable-line padding-line-between-statements
        return {
          values: {
            SYSTEM_INPUTS: Routes.SYSTEM.INPUTS,
          },
        };
      }

      default:
        return undefined;
    }
  }
}

export default NotificationsFactory;
