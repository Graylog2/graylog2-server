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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import CreateProfile from 'components/indices/IndexSetFieldTypeProfiles/CreateProfile';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';
import { simpleFields } from 'fixtures/fields';
import useLocation from 'routing/useLocation';
import { requestBodyProfile1JSON } from 'fixtures/indexSetFieldTypeProfiles';

const renderCreateNewProfile = () => render(<CreateProfile />);

jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

jest.mock('routing/useLocation', () => ({
  __esModule: true,
  default: jest.fn(),
}));

describe('CreateProfile', () => {
  const createMock = jest.fn(() => Promise.resolve(requestBodyProfile1JSON));
  const editMock = jest.fn(() => Promise.resolve());
  const deleteMock = jest.fn(() => Promise.resolve());

  useViewsPlugin();

  beforeEach(() => {
    asMock(useLocation).mockReturnValue({ pathname: '/', search: '', hash: '', key: 'default', state: undefined });

    asMock(useFieldTypesForMappings).mockReturnValue({
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
          ip: 'IP',
          date: 'Date',
        },
      },
      isLoading: false,
    });

    asMock(useProfileMutations).mockReturnValue({
      editProfile: editMock,
      isEditLoading: false,
      createProfile: createMock,
      isCreateLoading: false,
      isLoading: false,
      deleteProfile: deleteMock,
    });

    asMock(useFieldTypes).mockImplementation(() => ({ data: simpleFields().toArray(), refetch: jest.fn() }));
  });

  it('Run createProfile with form data', async () => {
    renderCreateNewProfile();

    const name = await screen.findByRole('textbox', {
      name: /name/i,
    });
    const description = await screen.findByRole('textbox', {
      name: /description/i,
    });
    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    await userEvent.click(addMappingButton);

    const submitButton = await screen.findByTitle(/create profile/i);

    await userEvent.type(name, 'Profile new');
    await userEvent.type(description, 'Profile description');

    await selectEvent.chooseOption('select customFieldMappings.0.field', 'date');
    await selectEvent.chooseOption('select customFieldMappings.0.type', 'String type');
    await selectEvent.chooseOption('select customFieldMappings.1.field', 'http_method');
    await selectEvent.chooseOption('select customFieldMappings.1.type', 'String type');

    await waitFor(() => expect(submitButton.hasAttribute('disabled')).toBe(false));
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledWith({
        name: 'Profile new',
        description: 'Profile description',
        customFieldMappings: [
          { field: 'date', type: 'string' },
          { field: 'http_method', type: 'string' },
        ],
      });
    });
  });

  it('Pre-fills name, description and mappings passed from a duplicated profile', async () => {
    asMock(useLocation).mockReturnValue({
      pathname: '',
      search: '',
      hash: '',
      key: 'duplicate',
      state: {
        name: 'Profile 1 (COPY)',
        description: 'Description 1',
        customFieldMappings: [{ field: 'http_method', type: 'string' }],
      },
    } as any);

    renderCreateNewProfile();

    expect(await screen.findByRole('textbox', { name: /name/i })).toHaveValue('Profile 1 (COPY)');
    expect(await screen.findByRole('textbox', { name: /description/i })).toHaveValue('Description 1');
  });

  it('Tells how many fields an import left out', async () => {
    asMock(useLocation).mockReturnValue({
      pathname: '',
      search: '',
      hash: '',
      key: 'import',
      state: {
        customFieldMappings: [{ field: 'http_method', type: 'string' }],
        skippedFieldsCount: 3,
      },
    } as any);

    renderCreateNewProfile();

    await screen.findByText(/3 fields were left out/i);
  });
});
