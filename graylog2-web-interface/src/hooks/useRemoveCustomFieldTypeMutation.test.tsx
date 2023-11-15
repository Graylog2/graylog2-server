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

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import useRemoveCustomFieldTypeMutation from 'hooks/useRemoveCustomFieldTypeMutation';

const urlPrefix = '/system/indices/mappings/remove_mapping';

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/UserNotification', () => ({
  error: jest.fn((...rest) => console.log('EDDDDDDDDDDDDDDDDD', { rest })),
  success: jest.fn(),
}));

describe('useRemoveCustomFieldTypeMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('removeCustomFieldTypeMutation', () => {
    const putUrl = qualifyUrl(`${urlPrefix}`);
    const requestBody = { rotated: true, fields: ['field'], indexSets: ['001'] };

    const requestBodyJSON = {
      index_sets: ['001'],
      rotate: true,
      fields: ['field'],
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));
      const { result, waitFor } = renderHook(() => useRemoveCustomFieldTypeMutation(), { queryClientOptions: { logger } });

      act(() => {
        result.current.removeCustomFieldTypeMutation(requestBody);
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('PUT', putUrl, requestBodyJSON));

      await waitFor(() => expect(UserNotification.success).toHaveBeenCalledWith('Custom field type removed successfully', 'Success!'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useRemoveCustomFieldTypeMutation(), { queryClientOptions: { logger } });

      act(() => {
        result.current.removeCustomFieldTypeMutation(requestBody);
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Removing custom field type failed with status: Error: Error',
        'Could not remove custom field type'));
    });
  });
});
