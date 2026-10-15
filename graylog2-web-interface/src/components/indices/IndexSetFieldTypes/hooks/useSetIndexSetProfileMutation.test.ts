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
import useSetIndexSetProfileMutation from 'components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation';

jest.mock('@graylog/server-api', () => ({ SystemFieldTypes: { bulkSetProfile: jest.fn(() => Promise.resolve({})) } }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useSetIndexSetProfileMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('setIndexSetFieldTypeProfile', () => {
    const requestBody = { rotated: true, profileId: 'profile-id-111', indexSetIds: ['001', '002'] };

    const requestBodyJSON = {
      index_sets: ['001', '002'],
      rotate: true,
      profile_id: 'profile-id-111',
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemFieldTypes.bulkSetProfile).mockImplementation(() =>
        Promise.resolve({
          '001': { successfully_performed: 1, failures: [], errors: [] },
          '002': { successfully_performed: 1, failures: [], errors: [] },
        }),
      );
      const { result } = renderHook(() => useSetIndexSetProfileMutation());

      act(() => {
        result.current.setIndexSetFieldTypeProfile(requestBody);
      });

      await waitFor(() => expect(SystemFieldTypes.bulkSetProfile).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith('Set index set profile successfully', 'Success!'),
      );
    });

    it('should resolve with the result per index set and skip the success notification on failures', async () => {
      asMock(SystemFieldTypes.bulkSetProfile).mockImplementation(() =>
        Promise.resolve({
          '001': { successfully_performed: 1, failures: [], errors: ['Profile change applied, but rotation failed'] },
          '002': {
            successfully_performed: 0,
            failures: [{ entity_id: '002', failure_explanation: 'Not authorized' }],
            errors: [],
          },
        }),
      );
      const { result } = renderHook(() => useSetIndexSetProfileMutation());

      let response: Awaited<ReturnType<typeof result.current.setIndexSetFieldTypeProfile>>;

      await act(async () => {
        response = await result.current.setIndexSetFieldTypeProfile(requestBody);
      });

      expect(response).toEqual({
        '001': {
          indexSetId: '001',
          applied: true,
          failures: [],
          errors: ['Profile change applied, but rotation failed'],
        },
        '002': { indexSetId: '002', applied: false, failures: ['Not authorized'], errors: [] },
      });
      expect(UserNotification.success).not.toHaveBeenCalled();
    });

    it('should display notification on fail', async () => {
      asMock(SystemFieldTypes.bulkSetProfile).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result } = renderHook(() => useSetIndexSetProfileMutation());

      act(() => {
        result.current.setIndexSetFieldTypeProfile(requestBody).catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Setting index set profile failed with status: Error: Error',
          'Could not set index set profile',
        ),
      );
    });
  });
});
