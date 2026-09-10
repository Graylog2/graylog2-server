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
import type * as Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

import { isPermitted } from 'util/PermissionsMixin';
import AppConfig from 'util/AppConfig';

/**
 * Whether a navigation item is available to the current user. Shared by the expanded and the
 * collapsed navigation menu so that both show exactly the same set of items.
 */
const shouldRenderNavigationItem = (
  requiredFeatureFlag: string | undefined,
  requiredPermissions: Permission | Array<Permission> | undefined,
  userPermissions: Immutable.List<Permission>,
) => {
  if (requiredFeatureFlag && !AppConfig.isFeatureEnabled(requiredFeatureFlag)) {
    return false;
  }

  if (requiredPermissions && !isPermitted(userPermissions, requiredPermissions)) {
    return false;
  }

  return true;
};

export default shouldRenderNavigationItem;
