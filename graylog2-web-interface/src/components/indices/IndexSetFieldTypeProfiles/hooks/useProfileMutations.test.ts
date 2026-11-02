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
import omit from 'lodash/omit';

import { SystemIndexSetsFieldTypeProfiles } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';
import { formValuesProfile1, requestBodyProfile1JSON } from 'fixtures/indexSetFieldTypeProfiles';

jest.mock('@graylog/server-api', () => ({
  SystemIndexSetsFieldTypeProfiles: { remove: jest.fn(), update: jest.fn(), create: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useProfileMutations', () => {
  describe('editProfile', () => {
    const requestBody = { profile: formValuesProfile1, id: '111' };

    const requestBodyJSON = requestBodyProfile1JSON;

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.update).mockResolvedValue(undefined);

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.editProfile(requestBody);
      });

      await waitFor(() => expect(SystemIndexSetsFieldTypeProfiles.update).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith(
          'Index set field type profile has been successfully updated.',
          'Success!',
        ),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.update).mockRejectedValue(new Error('Error'));

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.editProfile(requestBody).catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Updating index set field type profile failed with status: Error: Error',
          'Could not update index set field type profile',
        ),
      );
    });
  });

  describe('createProfile', () => {
    const requestBody = formValuesProfile1;

    const requestBodyJSON = omit(requestBodyProfile1JSON, ['id']);

    it('should run fetch and display UserNotification', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.create).mockResolvedValue({ ...requestBodyProfile1JSON, id: '111' });

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.createProfile(requestBody);
      });

      await waitFor(() => expect(SystemIndexSetsFieldTypeProfiles.create).toHaveBeenCalledWith(requestBodyJSON));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith(
          'Index set field type profile has been successfully created.',
          'Success!',
        ),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.create).mockRejectedValue(new Error('Error'));

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.createProfile(requestBody).catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Creating index set field type profile failed with status: Error: Error',
          'Could not create index set field type profile',
        ),
      );
    });
  });

  describe('deleteProfile', () => {
    it('should run fetch and display UserNotification', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.remove).mockResolvedValue(undefined);

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.deleteProfile('111');
      });

      await waitFor(() => expect(SystemIndexSetsFieldTypeProfiles.remove).toHaveBeenCalledWith('111'));

      await waitFor(() =>
        expect(UserNotification.success).toHaveBeenCalledWith(
          'Index set field type profile has been successfully deleted.',
          'Success!',
        ),
      );
    });

    it('should display notification on fail', async () => {
      asMock(SystemIndexSetsFieldTypeProfiles.remove).mockRejectedValue(new Error('Error'));

      const { result } = renderHook(() => useProfileMutations());

      act(() => {
        result.current.deleteProfile('111').catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Deleting index set field type profile failed with status: Error: Error',
          'Could not delete index set field type profile',
        ),
      );
    });
  });
});
