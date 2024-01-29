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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';
import omit from 'lodash/omit';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';
import { formValuesProfile1, requestBodyProfile1JSON } from 'fixtures/indexSetFieldTypeProfiles';

const urlPrefix = '/system/indices/index_sets/profiles';

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useProfileMutations', () => {
  describe('editProfile', () => {
    const putUrl = qualifyUrl(`${urlPrefix}`);
    const requestBody = { profile: formValuesProfile1, id: '111' };

    const requestBodyJSON = requestBodyProfile1JSON;

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.editProfile(requestBody);
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('PUT', putUrl, requestBodyJSON));

      await waitFor(() => expect(UserNotification.success).toHaveBeenCalledWith('Index set field type profile has been successfully updated.', 'Success!'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.editProfile(requestBody).catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Updating index set field type profile failed with status: Error: Error',
        'Could not update index set field type profile'));
    });
  });

  describe('createProfile', () => {
    const postUrl = qualifyUrl(`${urlPrefix}`);
    const requestBody = formValuesProfile1;

    const requestBodyJSON = omit(requestBodyProfile1JSON, ['id']);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.createProfile(requestBody);
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', postUrl, requestBodyJSON));

      await waitFor(() => expect(UserNotification.success).toHaveBeenCalledWith('Index set field type profile has been successfully created.', 'Success!'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.createProfile(requestBody).catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Creating index set field type profile failed with status: Error: Error',
        'Could not create index set field type profile'));
    });
  });

  describe('deleteProfile', () => {
    const deleteUrl = qualifyUrl(`${urlPrefix}/111`);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.deleteProfile('111');
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('DELETE', deleteUrl));

      await waitFor(() => expect(UserNotification.success).toHaveBeenCalledWith('Index set field type profile has been successfully deleted.', 'Success!'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useProfileMutations(), { queryClientOptions: { logger } });

      act(() => {
        result.current.deleteProfile('111').catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Deleting index set field type profile failed with status: Error: Error',
        'Could not delete index set field type profile'));
    });
  });
});
