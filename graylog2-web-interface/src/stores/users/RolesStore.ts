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
// @flow strict
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import type { UserJSON } from 'logic/users/User';

type Role = {
  name: string,
  description: string,
  permissions: string[],
};

type RoleMembership = {
  role: string,
  users: UserJSON[],
};

const RolesStore = Reflux.createStore({
  loadRoles(): Promise<string[]> {
    return fetch('GET', qualifyUrl(ApiRoutes.RolesApiController.listRoles().url))
      .then(
        (response) => response.roles,
        (error) => {
          if (error.additional.status !== 404) {
            UserNotification.error(`Loading role list failed with status: ${error}`,
              'Could not load role list');
          }
        },
      );
  },

  createRole(role: Role): Promise<Role> {
    const url = qualifyUrl(ApiRoutes.RolesApiController.createRole().url);
    const promise = fetch('POST', url, role);

    promise.then((newRole) => {
      UserNotification.success(`Role "${newRole.name}" was created successfully`);
    }, (error) => {
      UserNotification.error(`Creating role "${role.name}" failed with status: ${error}`,
        'Could not create role');
    });

    return promise;
  },

  updateRole(rolename: string, role: Role): Promise<Role> {
    const promise = fetch('PUT', qualifyUrl(ApiRoutes.RolesApiController.updateRole(encodeURIComponent(rolename)).url), role);

    promise.then((newRole) => {
      UserNotification.success(`Role "${newRole.name}" was updated successfully`);
    }, (error) => {
      if (error.additional.status !== 404) {
        UserNotification.error(`Updating role failed with status: ${error}`,
          'Could not update role');
      }
    });

    return promise;
  },

  deleteRole(rolename: string): Promise<string[]> {
    const url = qualifyUrl(ApiRoutes.RolesApiController.deleteRole(encodeURIComponent(rolename)).url);
    const promise = fetch('DELETE', url);

    promise.then(() => {
      UserNotification.success(`Role "${rolename}" was deleted successfully`);
    }, (error) => {
      if (error.additional.status !== 404) {
        UserNotification.error(`Deleting role failed with status: ${error}`,
          'Could not delete role');
      }
    });

    return promise;
  },

  getMembers(rolename: string): Promise<RoleMembership[]> {
    const url = qualifyUrl(ApiRoutes.RolesApiController.loadMembers(encodeURIComponent(rolename)).url);
    const promise = fetch('GET', url);

    promise.catch((error) => {
      if (error.additional.status !== 404) {
        UserNotification.error(`Could not load role's members with status: ${error}`,
          'Could not load role members');
      }
    });

    return promise;
  },
});

export default RolesStore;
