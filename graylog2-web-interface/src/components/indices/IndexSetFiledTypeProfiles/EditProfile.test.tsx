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
import { render, screen, fireEvent, act } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import EditProfile from 'components/indices/IndexSetFiledTypeProfiles/EditProfile';
import useProfileMutations from 'components/indices/IndexSetFiledTypeProfiles/hooks/useProfileMutations';
import { simpleFields } from 'fixtures/fields';
import { profile1 } from 'fixtures/indexSetFieldTypeProfiles';

const renderEditProfile = () => render(
  <EditProfile profile={profile1} />,
);

jest.mock('components/indices/IndexSetFiledTypeProfiles/hooks/useProfileMutations', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

const selectItem = async (select: HTMLElement, option: string | RegExp) => {
  selectEvent.openMenu(select);

  return selectEvent.select(select, option);
};

describe('IndexSetFieldTypesList', () => {
  const createMock = jest.fn(() => Promise.resolve());
  const editMock = jest.fn(() => Promise.resolve());

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useFieldTypesForMappings).mockReturnValue({
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
          ip: 'IP',
        },
      },
      isLoading: false,
    });

    asMock(useProfileMutations).mockReturnValue(({
      editProfile: editMock,
      isEditLoading: false,
      createProfile: createMock,
      isCreateLoading: false,
      isLoading: false,
    }));

    asMock(useFieldTypes).mockImplementation(() => (
      { data: simpleFields().toArray(), refetch: jest.fn() }
    ));
  });

  it('Run editProfile with changed form data', async () => {
    renderEditProfile();

    const name = await screen.findByRole('textbox', {
      name: /name/i,
      hidden: true,
    });

    const fieldFirst = await screen.findByLabelText(/select customFieldMappings.0.field/i);
    const typeFirst = await screen.findByLabelText(/select customFieldMappings.0.type/i);
    const submitButton = await screen.findByTitle(/update profile/i);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      userEvent.paste(name, ' new name');
      await selectItem(fieldFirst, 'date');
      await selectItem(typeFirst, 'String type');
      fireEvent.click(submitButton);
    });

    expect(editMock).toHaveBeenCalledWith({
      name: 'Profile 1 new name',
      description: 'Description 1',
      id: '111',
      customFieldMappings: [
        { field: 'date', type: 'string' },
        { field: 'user_ip', type: 'ip' },
      ],
    });
  });

  it('Run editProfile with added form data', async () => {
    renderEditProfile();

    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      fireEvent.click(addMappingButton);
    });

    const fieldThird = await screen.findByLabelText(/select customFieldMappings.2.field/i);
    const typeThird = await screen.findByLabelText(/select customFieldMappings.2.type/i);
    const submitButton = await screen.findByTitle(/update profile/i);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await selectItem(fieldThird, 'date');
      await selectItem(typeThird, 'String type');
      fireEvent.click(submitButton);
    });

    expect(editMock).toHaveBeenCalledWith({
      name: 'Profile 1',
      description: 'Description 1',
      id: '111',
      customFieldMappings: [
        { field: 'http_method', type: 'string' },
        { field: 'user_ip', type: 'ip' },
        { field: 'date', type: 'string' },
      ],
    });
  });
});
