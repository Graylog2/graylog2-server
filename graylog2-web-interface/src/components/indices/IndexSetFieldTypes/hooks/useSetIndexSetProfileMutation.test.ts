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

jest.mock('@graylog/server-api', () => ({ SystemFieldTypes: { setProfile: jest.fn(() => Promise.resolve()) } }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useRemoveCustomFieldTypeMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('removeCustomFieldTypeMutation', () => {
    const requestBody = { rotated: true, profileId: 'profile-id-111', indexSetId: '001' };

    const requestBodyJSON = {
      index_sets: ['001'],
      rotate: true,
      profile_id: 'profile-id-111',
    };

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemFieldTypes.setProfile).mockImplementation(() => Promise.resolve({}));
      const { result } = renderHook(() => useSetIndexSetProfileMutation());

      act(() => {
        result.current.setIndexSetFieldTypeProfile(requestBody);
      });

      await waitFor(() => expect(SystemFieldTypes.setProfile).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith('Set index set profile successfully', 'Success!'),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemFieldTypes.setProfile).mockImplementation(() => Promise.reject(new Error('Error')));

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
