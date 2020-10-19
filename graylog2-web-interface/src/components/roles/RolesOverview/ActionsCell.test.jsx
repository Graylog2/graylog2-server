// @flow strict
import * as React from 'react';
import asMock from 'helpers/mocking/AsMock';
import { render, waitFor, fireEvent, act } from 'wrappedTestingLibrary';
import { paginatedUsers } from 'fixtures/userOverviews';
import { viewsManager } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import ActionsCell from './ActionsCell';

const mockLoadUsersPromise = Promise.resolve();

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
      const { getByRole } = render(renderSUT({
        permissions: userPermissions,
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      const deleteButton = getByRole('button', { name: `Delete role ${customRoleName}` });
      fireEvent.click(deleteButton);
      await act(() => mockLoadUsersPromise);

      expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete role "${customRoleName}"?`);

      await waitFor(() => expect(AuthzRolesActions.delete).toHaveBeenCalledWith(customRoleId, customRoleName));
    });

    it('should display confirm dialog which includes information about assigned users', async () => {
      const confirmMessage = `Do you really want to delete role "${customRoleName}"?\n\nIt is still assigned to ${paginatedUsers.list.size} users.`;
      const mockLoadManyUsersPromise = Promise.resolve(paginatedUsers);
      asMock(AuthzRolesActions.loadUsersForRole).mockReturnValueOnce(mockLoadManyUsersPromise);
      const userPermissions = [
        `roles:edit:${customRoleName}`,
        `roles:delete:${customRoleName}`,
      ];
      const { getByRole } = render(renderSUT({
        permissions: userPermissions,
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      const deleteButton = getByRole('button', { name: `Delete role ${customRoleName}` });
      fireEvent.click(deleteButton);
      await act(() => mockLoadManyUsersPromise);

      expect(window.confirm).toHaveBeenCalledWith(confirmMessage);

      await waitFor(() => expect(AuthzRolesActions.delete).toHaveBeenCalledWith(customRoleId, customRoleName));
    });

    it('should not be possible for built in roles', async () => {
      const builtInRoleName = 'built-in-role-name';
      const builtInRoleId = 'built-in-role-id';
      const userPermissions = [
        `roles:edit:${builtInRoleName}`,
        `roles:delete:${builtInRoleName}`,
      ];
      const { queryByRole } = render(renderSUT({
        permissions: userPermissions,
        readOnly: true,
        roleId: builtInRoleId,
        roleName: builtInRoleName,
      }));

      // Ensure that the component rendered correctly
      expect(queryByRole('button', { name: `Edit role ${builtInRoleName}` })).not.toBeNull();
      expect(queryByRole('button', { name: `Delete role ${builtInRoleName}` })).toBeNull();
    });

    it('should not be possible if user does not have correct permissions', async () => {
      const { queryByRole } = render(renderSUT({
        permissions: [],
        readOnly: false,
        roleId: customRoleId,
        roleName: customRoleName,
      }));

      expect(queryByRole('button', { name: `Edit role ${customRoleName}` })).toBeNull();
      expect(queryByRole('button', { name: `Delete role ${customRoleName}` })).toBeNull();
    });
  });
});
