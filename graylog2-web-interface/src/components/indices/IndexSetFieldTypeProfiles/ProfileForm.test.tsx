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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import asMock from 'helpers/mocking/AsMock';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import ProfileForm from 'components/indices/IndexSetFieldTypeProfiles/ProfileForm';
import { simpleFields } from 'fixtures/fields';
import { profile1 } from 'fixtures/indexSetFieldTypeProfiles';

const mockSubmit = jest.fn();
const mockCancel = jest.fn();
const renderProfileForm = ({ initialValues }) => render(
  <ProfileForm onCancel={mockCancel} onSubmit={mockSubmit} submitLoadingText="Submitting..." submitButtonText="Submit" initialValues={initialValues} />,
);
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

const selectItem = async (select: HTMLElement, option: string | RegExp) => {
  selectEvent.openMenu(select);

  return selectEvent.select(select, option);
};

describe('IndexSetFieldTypesList', () => {
  useViewsPlugin();

  beforeEach(() => {
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

    asMock(useFieldTypes).mockImplementation(() => (
      { data: simpleFields().toArray(), refetch: jest.fn() }
    ));
  });

  it('Do not run onSubmit when has empty name', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        name: '',
      },
    });

    const submitButton = await screen.findByLabelText('Submit');
    fireEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('Do not run onSubmit when has empty customFieldMapping', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        customFieldMappings: [
          { field: 'http_method', type: 'string' },
        ],
      },
    });

    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    fireEvent.click(addMappingButton);

    const typeSecond = await screen.findByLabelText(/select customFieldMappings.1.type/i);
    const submitButton = await screen.findByLabelText('Submit');

    await selectItem(typeSecond, 'String type');

    await waitFor(async () => {
      expect(screen.queryAllByText('String type')).toHaveLength(2);
    });

    fireEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('Do not run onSubmit when has same fields in customFieldMapping', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        customFieldMappings: [
          { field: 'http_method', type: 'string' },
        ],
      },
    });

    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    fireEvent.click(addMappingButton);

    const fieldSecond = await screen.findByLabelText(/select customFieldMappings.1.field/i);
    const typeSecond = await screen.findByLabelText(/select customFieldMappings.1.type/i);
    const submitButton = await screen.findByLabelText('Submit');

    await selectItem(typeSecond, 'String type');
    await selectItem(fieldSecond, 'http_method');

    await waitFor(async () => {
      expect(screen.queryAllByText('http_method')).toHaveLength(2);
    });

    fireEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });
});
