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
import * as React from 'react';
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import selectEvent from 'helpers/selectEvent';
import useSetIndexSetProfileMutation from 'components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation';
import asMock from 'helpers/mocking/AsMock';
import SetProfileModal from 'components/indices/IndexSetFieldTypes/SetProfileModal';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useRemoveProfileFromIndexMutation from 'components/indices/IndexSetFieldTypes/hooks/useRemoveProfileFromIndexMutation';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import type { ProfileTargetIndexSet } from 'components/indices/IndexSetFieldTypes/profileTargets';
import type { ProfileChangeResponse } from 'components/indices/IndexSetFieldTypes/types';

jest.mock('components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions');
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfile');
jest.mock('components/indices/IndexSetFieldTypes/hooks/useRemoveProfileFromIndexMutation');
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings');

const userIndexSet: ProfileTargetIndexSet = {
  id: '111',
  title: 'User index set',
  writable: true,
  field_type_profile: 'profile-id-111',
  can_have_profile: true,
};
const readOnlyIndexSet: ProfileTargetIndexSet = {
  id: '222',
  title: 'Archived index set',
  writable: false,
  field_type_profile: null,
  can_have_profile: true,
};
const eventsIndexSet: ProfileTargetIndexSet = {
  id: '333',
  title: 'Events index set',
  writable: true,
  field_type_profile: null,
  can_have_profile: false,
};

const applied = (indexSetId: string, errors: Array<string> = []) => ({
  indexSetId,
  applied: true,
  failures: [],
  errors,
});
const failed = (indexSetId: string, explanation: string) => ({
  indexSetId,
  applied: false,
  failures: [explanation],
  errors: [],
});

describe('SetProfileModal', () => {
  const setIndexSetFieldTypeProfileMock = jest.fn((): Promise<ProfileChangeResponse> => Promise.resolve({}));
  const removeProfileFromIndexMock = jest.fn((): Promise<ProfileChangeResponse> => Promise.resolve({}));
  const onClose = jest.fn();
  const onChangeApplied = jest.fn();

  const renderModal = (props: Partial<React.ComponentProps<typeof SetProfileModal>> = {}) =>
    render(
      <SetProfileModal
        indexSets={[userIndexSet]}
        currentProfile="profile-id-111"
        onClose={onClose}
        onChangeApplied={onChangeApplied}
        show
        {...props}
      />,
    );

  const submit = async (name: RegExp) => userEvent.click(await screen.findByRole('button', { name }));

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useProfileOptions).mockReturnValue({
      options: [
        { value: 'profile-id-111', label: 'Profile-1' },
        { value: 'profile-id-222', label: 'Profile-2' },
      ],
      isLoading: false,
      refetch: () => {},
    });

    asMock(useProfile).mockImplementation((id: string) => ({
      data: {
        id,
        name: 'Profile',
        description: `Description of ${id}`,
        customFieldMappings: [{ field: 'http_method', type: 'string' }],
        indexSetIds: ['111', '444'],
      },
      isFetched: true,
      isFetching: false,
      refetch: () => {},
    }));

    asMock(useFieldTypesForMappings).mockReturnValue({
      data: { fieldTypes: { string: 'String (aggregatable)' } },
      isLoading: false,
    });

    asMock(useSetIndexSetProfileMutation).mockReturnValue({
      setIndexSetFieldTypeProfile: setIndexSetFieldTypeProfileMock,
      isLoading: false,
    });

    asMock(useRemoveProfileFromIndexMutation).mockReturnValue({
      removeProfileFromIndex: removeProfileFromIndexMock,
      isLoading: false,
    });
  });

  it('run setIndexSetFieldTypeProfile on submit with rotation', async () => {
    renderModal();
    await selectEvent.chooseOption('Select index set profile', 'Profile-2');
    await submit(/Set Profile/i);

    expect(setIndexSetFieldTypeProfileMock).toHaveBeenCalledWith({
      profileId: 'profile-id-222',
      indexSetIds: ['111'],
      rotated: true,
    });
  });

  it('run setIndexSetFieldTypeProfile on submit without rotation', async () => {
    renderModal();
    await selectEvent.chooseOption('Select index set profile', 'Profile-2');
    await userEvent.click(await screen.findByRole('checkbox', { name: /rotate affected indices after change/i }));
    await submit(/Set Profile/i);

    expect(setIndexSetFieldTypeProfileMock).toHaveBeenCalledWith({
      profileId: 'profile-id-222',
      indexSetIds: ['111'],
      rotated: false,
    });
  });

  it('run removeProfileFromIndex on submit without rotation', async () => {
    renderModal();
    await userEvent.click(await screen.findByRole('checkbox', { name: /rotate affected indices after change/i }));
    await submit(/Remove profile/i);

    expect(removeProfileFromIndexMock).toHaveBeenCalledWith({
      indexSetIds: ['111'],
      rotated: false,
    });
  });

  it('run removeProfileFromIndex on submit with rotation', async () => {
    renderModal();
    await submit(/Remove profile/i);

    expect(removeProfileFromIndexMock).toHaveBeenCalledWith({
      indexSetIds: ['111'],
      rotated: true,
    });
  });

  it('render modal without removal button when profile is not set', async () => {
    renderModal({ currentProfile: null });
    await screen.findByRole('button', { name: /Set Profile/i });

    expect(screen.queryByRole('button', { name: /Remove profile/i })).not.toBeInTheDocument();
  });

  it('shows the details of the selected profile', async () => {
    renderModal({ currentProfile: null });
    await selectEvent.chooseOption('Select index set profile', 'Profile-2');

    await screen.findByText('Description of profile-id-222');
    await screen.findByText(/already used by 2 index sets/i);
    await screen.findByRole('row', { name: /http_method string \(aggregatable\)/i });
  });

  it('lists affected and skipped index sets', async () => {
    renderModal({ indexSets: [userIndexSet, readOnlyIndexSet, eventsIndexSet], currentProfile: null });

    await screen.findByText('Applies to 2 index sets, excludes 1.');
    await screen.findByRole('row', { name: /user index set included/i });
    await screen.findByRole('row', { name: /archived index set included read-only, not rotated/i });
    await screen.findByRole('row', {
      name: /events index set excluded field types of this index set type cannot be changed/i,
    });

    await selectEvent.chooseOption('Select index set profile', 'Profile-2');
    await submit(/Set Profile/i);

    expect(setIndexSetFieldTypeProfileMock).toHaveBeenCalledWith({
      profileId: 'profile-id-222',
      indexSetIds: ['111', '222'],
      rotated: true,
    });
  });

  it('only removes profiles from index sets which have one', async () => {
    renderModal({ indexSets: [userIndexSet, readOnlyIndexSet], action: 'remove', currentProfile: null });

    await screen.findByText('Applies to 1 index set, excludes 1.');
    await screen.findByRole('row', { name: /archived index set excluded no field type profile set/i });

    expect(screen.queryByLabelText('Select profile')).not.toBeInTheDocument();

    await submit(/Remove profile/i);

    expect(removeProfileFromIndexMock).toHaveBeenCalledWith({ indexSetIds: ['111'], rotated: true });
  });

  it('closes after every index set was changed', async () => {
    const response = { 111: applied('111') };
    setIndexSetFieldTypeProfileMock.mockResolvedValue(response);
    renderModal();

    await selectEvent.chooseOption('Select index set profile', 'Profile-2');
    await submit(/Set Profile/i);

    expect(onChangeApplied).toHaveBeenCalledWith(response);
    expect(onClose).toHaveBeenCalled();
  });

  it('stays open and reports index sets which failed', async () => {
    const response = {
      111: applied('111', ['Profile change applied, but rotation failed: timeout']),
      222: failed('222', 'Not authorized'),
    };
    setIndexSetFieldTypeProfileMock.mockResolvedValue(response);
    renderModal({ indexSets: [userIndexSet, readOnlyIndexSet], currentProfile: null });

    await selectEvent.chooseOption('Select index set profile', 'Profile-2');
    await submit(/Set Profile/i);

    const alert = await screen.findByRole('alert');
    within(alert).getByText('Archived index set');
    within(alert).getByText('Not authorized');
    within(alert).getByText('User index set');
    within(alert).getByText('Profile change applied, but rotation failed: timeout');

    expect(onChangeApplied).toHaveBeenCalledWith(response);
    expect(onClose).not.toHaveBeenCalled();
  });
});
