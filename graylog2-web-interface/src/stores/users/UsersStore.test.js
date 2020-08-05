// @flow strict
import asMock from 'helpers/mocking/AsMock';
import { userList } from 'fixtures/users';

import { UsersActions } from 'stores/users/UsersStore';
import fetch from 'logic/rest/FetchProvider';

describe('UsersStore', () => {
  it('should load json users and store them as value classes', async (done) => {
    const jsonList = userList.map((u) => u.toJSON()).toArray();
    asMock(fetch).mockReturnValueOnce(Promise.resolve({ users: jsonList }));

    UsersActions.loadUsers().then((result) => {
      expect(result).toStrictEqual(userList);

      done();
    });
  });
});
