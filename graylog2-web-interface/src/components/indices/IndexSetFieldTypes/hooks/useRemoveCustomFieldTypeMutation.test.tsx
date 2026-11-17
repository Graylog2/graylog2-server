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
import useRemoveCustomFieldTypeMutation from 'components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation';
import { overriddenIndexField, overriddenIndexFieldJson } from 'fixtures/indexSetFieldTypes';

jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: { removeCustomMapping: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useRemoveCustomFieldTypeMutation', () => {
  const mockOnSuccessHandler = jest.fn();
  const mockOnErrorHandler = jest.fn();

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('removeCustomFieldTypeMutation', () => {
    const requestBody = { rotated: true, fields: ['field'], indexSets: ['001'] };

    const requestBodyJSON = {
      index_sets: ['001'],
      rotate: true,
      fields: ['field'],
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemFieldTypes.removeCustomMapping).mockResolvedValue({
        '001': {
          succeeded: [overriddenIndexFieldJson],
          failures: [],
          successfully_performed: 1,
          errors: [],
        },
      });

      const { result } = renderHook(() =>
        useRemoveCustomFieldTypeMutation({
          onSuccessHandler: mockOnSuccessHandler,
          onErrorHandler: mockOnErrorHandler,
        }),
      );

      act(() => {
        result.current.removeCustomFieldTypeMutation(requestBody);
      });

      await waitFor(() => expect(SystemFieldTypes.removeCustomMapping).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(mockOnSuccessHandler).toHaveBeenCalledWith({
          '001': {
            indexSetId: '001',
            succeeded: [overriddenIndexField],
            failures: [],
            errors: [],
            successfullyPerformed: 1,
          },
        }),
      );

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith('Custom field type removed successfully', 'Success!'),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemFieldTypes.removeCustomMapping).mockRejectedValue(new Error('Error'));

      const { result } = renderHook(() =>
        useRemoveCustomFieldTypeMutation({
          onSuccessHandler: mockOnSuccessHandler,
          onErrorHandler: mockOnErrorHandler,
        }),
      );

      act(() => {
        result.current.removeCustomFieldTypeMutation(requestBody).catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Removing custom field type failed with status: Error: Error',
          'Could not remove custom field type',
        ),
      );
    });

    it('should run onErrorHandler when response has failures', async () => {
      asMock(SystemFieldTypes.removeCustomMapping).mockResolvedValue({
        '001': {
          successfully_performed: 0,
          failures: [{ entity_id: 'field', failure_explanation: 'field has error' }],
          succeeded: [],
          errors: ['Some error'],
        },
      });

      const { result } = renderHook(() =>
        useRemoveCustomFieldTypeMutation({
          onSuccessHandler: mockOnSuccessHandler,
          onErrorHandler: mockOnErrorHandler,
        }),
      );

      act(() => {
        result.current.removeCustomFieldTypeMutation(requestBody);
      });

      await waitFor(() =>
        expect(mockOnErrorHandler).toHaveBeenCalledWith({
          '001': {
            indexSetId: '001',
            succeeded: [],
            failures: [{ entityId: 'field', failureExplanation: 'field has error' }],
            errors: ['Some error'],
            successfullyPerformed: 0,
          },
        }),
      );
    });
  });
});
