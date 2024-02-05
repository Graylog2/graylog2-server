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
import useFieldTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';

const urlPrefix = '/system/indices/mappings';
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

describe('useFieldTypeMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('putFieldTypeMutation', () => {
    const putUrl = qualifyUrl(`${urlPrefix}`);
    const requestBody = { rotated: true, field: 'field', newFieldType: 'int', indexSetSelection: ['001'] };

    const requestBodyJSON = {
      index_sets: ['001'],
      type: 'int',
      rotate: true,
      field: 'field',
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));
      const { result, waitFor } = renderHook(() => useFieldTypeMutation(), { queryClientOptions: { logger } });

      act(() => {
        result.current.putFieldTypeMutation(requestBody);
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('PUT', putUrl, requestBodyJSON));

      await waitFor(() => expect(UserNotification.success).toHaveBeenCalledWith('The field type changed successfully', 'Success!'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useFieldTypeMutation(), { queryClientOptions: { logger } });

      act(() => {
        result.current.putFieldTypeMutation(requestBody).catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Changing the field type failed with status: Error: Error',
        'Could not change the field type'));
    });
  });
});
