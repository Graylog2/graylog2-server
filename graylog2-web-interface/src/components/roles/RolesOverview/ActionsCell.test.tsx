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
import * as React from 'react';
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';
import { paginatedUsers } from 'fixtures/userOverviews';
import { viewsManager } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import ActionsCell from './ActionsCell';

const mockLoadUsersPromise = Promise.resolve({ list: Immutable.List(), pagination: { perPage: 10, page: 1, total: 0 } });

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    delete: jest.fn(() => Promise.resolve()),
    loadUsersForRole: jest.fn(() => mockLoadUsersPromise),
  },
}));

describe('ActionsCell', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const customRoleName = 'custom-role-name';
  const customRoleId = 'custom-role-id';
  const currentUser = viewsManager;
  const renderSUT = ({ permissions, readOnly, roleName, roleId }) => (
    <CurrentUserContext.Provider value={{ ...currentUser, permissions }}>
      <table>
        <tbody>
          <tr>
            <ActionsCell readOnly={readOnly} roleName={roleName} roleId={roleId} />
          </tr>
        </tbody>
      </table>
    </CurrentUserContext.Provider>
  );

  describe('role deletion', () => {
    let oldConfirm;

    beforeEach(() => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => true);
    });

    afterEach(() => {
      window.confirm = oldConfirm;
      jest.clearAllMocks();
    });

    it('should be possible if role is not built in', async () => {
      const userPermissions = [
        `roles:edit:${customRoleName}`,
        `roles:delete:${customRoleName}`,
      ];

      render(renderSUT({
        permissions: userPermissions,
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      const deleteButton = screen.getByRole('button', { name: `Delete role ${customRoleName}` });
      fireEvent.click(deleteButton);

      await waitFor(() => expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete role "${customRoleName}"?`));

      expect(AuthzRolesActions.delete).toHaveBeenCalledWith(customRoleId, customRoleName);
    });

    it('should display confirm dialog which includes information about assigned users', async () => {
      const confirmMessage = `Do you really want to delete role "${customRoleName}"?\n\nIt is still assigned to ${paginatedUsers.list.size} users.`;
      const mockLoadManyUsersPromise = Promise.resolve(paginatedUsers);
      asMock(AuthzRolesActions.loadUsersForRole).mockReturnValueOnce(mockLoadManyUsersPromise);
      const userPermissions = [
        `roles:edit:${customRoleName}`,
        `roles:delete:${customRoleName}`,
      ];

      render(renderSUT({
        permissions: userPermissions,
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      const deleteButton = screen.getByRole('button', { name: `Delete role ${customRoleName}` });
      fireEvent.click(deleteButton);

      await waitFor(() => expect(window.confirm).toHaveBeenCalledWith(confirmMessage));

      expect(AuthzRolesActions.delete).toHaveBeenCalledWith(customRoleId, customRoleName);
    });

    it('should not be possible for built in roles', () => {
      const builtInRoleName = 'built-in-role-name';
      const builtInRoleId = 'built-in-role-id';
      const userPermissions = [
        `roles:edit:${builtInRoleName}`,
        `roles:delete:${builtInRoleName}`,
      ];

      render(renderSUT({
        permissions: userPermissions,
        readOnly: true,
        roleId: builtInRoleId,
        roleName: builtInRoleName,
      }));

      // Ensure that the component rendered correctly
      expect(screen.queryByTitle(`Edit role ${builtInRoleName}`)).toBeInTheDocument();
      expect(screen.queryByTitle(`Delete role ${builtInRoleName}`)).not.toBeInTheDocument();
    });

    it('should not be possible if user does not have correct permissions', () => {
      render(renderSUT({
        permissions: [],
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      expect(screen.queryByRole('button', { name: `Edit role ${customRoleName}` })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: `Delete role ${customRoleName}` })).not.toBeInTheDocument();
    });
  });
});
