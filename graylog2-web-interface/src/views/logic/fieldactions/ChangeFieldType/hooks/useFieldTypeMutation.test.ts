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
import { renderHook, act, waitFor } from 'wrappedTestingLibrary/hooks';

import { SystemFieldTypes } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import useFieldTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';

jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: { changeFieldType: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useFieldTypeMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('putFieldTypeMutation', () => {
    const requestBody = { rotated: true, field: 'field', newFieldType: 'int', indexSetSelection: ['001'] };

    const requestBodyJSON = {
      index_sets: ['001'],
      type: 'int',
      rotate: true,
      field: 'field',
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemFieldTypes.changeFieldType).mockResolvedValue({});
      const { result } = renderHook(() => useFieldTypeMutation());

      act(() => {
        result.current.putFieldTypeMutation(requestBody);
      });

      await waitFor(() => expect(SystemFieldTypes.changeFieldType).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith('The field type changed successfully', 'Success!'),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemFieldTypes.changeFieldType).mockRejectedValue(new Error('Error'));

      const { result } = renderHook(() => useFieldTypeMutation());

      act(() => {
        result.current.putFieldTypeMutation(requestBody).catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Changing the field type failed with status: Error: Error',
          'Could not change the field type',
        ),
      );
    });
  });
});
