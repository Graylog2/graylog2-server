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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import { QueryParamProvider } from 'use-query-params';
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';
import selectEvent from 'react-select-event';

import useSetIndexSetProfileMutation from 'components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation';
import useParams from 'routing/useParams';
import asMock from 'helpers/mocking/AsMock';
import SetProfileModal from 'components/indices/IndexSetFieldTypes/SetProfileModal';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';

const selectItem = async (select: HTMLElement, option: string | RegExp) => {
  selectEvent.openMenu(select);

  return selectEvent.select(select, option);
};

const renderModal = (currentProfile = 'profile-id-111') => render(
  <QueryParamProvider adapter={ReactRouter6Adapter}>
    <SetProfileModal currentProfile={currentProfile} onClose={() => {}} show />
  </QueryParamProvider>,
);

jest.mock('routing/useParams', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions');

describe('IndexSetFieldTypesList', () => {
  const setIndexSetFieldTypeProfileMock = jest.fn(() => Promise.resolve());

  beforeEach(() => {
    asMock(useParams).mockImplementation(() => ({
      indexSetId: '111',
    }));

    asMock(useProfileOptions).mockReturnValue({
      options: [
        { value: 'profile-id-111', label: 'Profile-1' },
        { value: 'profile-id-222', label: 'Profile-2' },
      ],
      isLoading: false,
      refetch: () => {},
    });

    asMock(useSetIndexSetProfileMutation).mockReturnValue({
      setIndexSetFieldTypeProfile: setIndexSetFieldTypeProfileMock,
      isLoading: false,
    });
  });

  it('run setIndexSetFieldTypeProfile on submit with rotation', async () => {
    renderModal();
    const select = await screen.findByLabelText(/Select profile/i);
    await selectItem(select, 'Profile-2');
    const submit = await screen.findByRole('button', { name: /Set Profile/i, hidden: true });
    fireEvent.click(submit);

    expect(setIndexSetFieldTypeProfileMock).toHaveBeenCalledWith({
      profileId: 'profile-id-222',
      indexSetId: '111',
      rotated: true,
    });
  });

  it('run setIndexSetFieldTypeProfile on submit without rotation', async () => {
    renderModal();
    const select = await screen.findByLabelText(/Select profile/i);
    await selectItem(select, 'Profile-2');
    const submit = await screen.findByRole('button', { name: /Set Profile/i, hidden: true });
    const checkBox = await screen.findByRole('checkbox', { name: /rotate affected indices after change/i, hidden: true });
    fireEvent.click(checkBox);
    fireEvent.click(submit);

    expect(setIndexSetFieldTypeProfileMock).toHaveBeenCalledWith({
      profileId: 'profile-id-222',
      indexSetId: '111',
      rotated: false,
    });
  });
});
