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
import CreateNewProfile from 'components/indices/IndexSetFiledTypeProfiles/CreateNewProfile';
import useProfileMutations from 'components/indices/IndexSetFiledTypeProfiles/hooks/useProfileMutations';
import { simpleFields } from 'fixtures/fields';

const renderCreateNewProfile = () => render(
  <CreateNewProfile />,
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

  it('Run createProfile with form data', async () => {
    renderCreateNewProfile();

    const name = await screen.findByRole('textbox', {
      name: /name/i,
      hidden: true,
    });
    const description = await screen.findByRole('textbox', {
      name: /description/i,
      hidden: true,
    });
    const fieldFirst = await screen.findByLabelText(/select customFieldMappings.0.field/i);
    const typeFirst = await screen.findByLabelText(/select customFieldMappings.0.type/i);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      userEvent.paste(name, 'Profile new');
      userEvent.paste(description, 'Profile description');
      await selectItem(fieldFirst, 'date');
      await selectItem(typeFirst, 'String type');
      const submitButton = await screen.findByTitle(/create new profile/i);
      fireEvent.click(submitButton);
    });

    expect(createMock).toHaveBeenCalledWith({
      name: 'Profile new',
      description: 'Profile description',
      customFieldMappings: [{ field: 'date', type: 'string' }],
    });
  });
});
