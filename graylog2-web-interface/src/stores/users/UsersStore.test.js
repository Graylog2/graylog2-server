// @flow strict
import asMock from 'helpers/mocking/AsMock';
import { userList } from 'fixtures/users';

import UsersActions from 'actions/users/UsersActions';
import fetch from 'logic/rest/FetchProvider';

describe('UsersStore', () => {
  it('should load json users and store them as value classes', async () => {
    const jsonList = userList.map((u) => u.toJSON()).toArray();
    asMock(fetch).mockReturnValueOnce(Promise.resolve({ users: jsonList }));

    UsersActions.loadUsers().then((users) => {
      expect(users).toBe(jsonList);
    });
  });
});
