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
import React from 'react';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import HideOnCloud from 'util/conditional/HideOnCloud';

class NotificationsFactory {
  static getForNotification(notification) {
  }

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

      case 'no_input_running': {
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
