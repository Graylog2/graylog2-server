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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import ProfileForm from 'components/indices/IndexSetFieldTypeProfiles/ProfileForm';
import { simpleFields } from 'fixtures/fields';
import { profile1 } from 'fixtures/indexSetFieldTypeProfiles';

const mockSubmit = jest.fn();
const mockCancel = jest.fn();
const buildMappings = (count: number) =>
  Array.from({ length: count }, (_, index) => ({ field: `field_${String(index).padStart(3, '0')}`, type: 'string' }));
const renderProfileForm = ({ initialValues }) =>
  render(
    <ProfileForm
      onCancel={mockCancel}
      onSubmit={mockSubmit}
      submitLoadingText="Submitting..."
      submitButtonText="Submit"
      initialValues={initialValues}
    />,
  );
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

describe('IndexSetFieldTypesList', () => {
  useViewsPlugin();

  beforeEach(() => {
    mockSubmit.mockClear();

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

    asMock(useFieldTypes).mockImplementation(() => ({ data: simpleFields().toArray(), refetch: jest.fn() }));
  });

  it('shows 20 mappings per page and pages through the rest', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(120) } });

    expect(await screen.findByText('Showing 1-20 of 120 mappings')).toBeInTheDocument();
    expect(screen.getByTestId('custom-mapping-row-for-field_019')).toBeInTheDocument();
    expect(screen.queryByTestId('custom-mapping-row-for-field_020')).not.toBeInTheDocument();

    await userEvent.click(await screen.findByRole('button', { name: /next page/i }));

    expect(await screen.findByText('Showing 21-40 of 120 mappings')).toBeInTheDocument();
    expect(screen.getByTestId('custom-mapping-row-for-field_020')).toBeInTheDocument();
  });

  it('sorts the mappings alphabetically by field name', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        customFieldMappings: [
          { field: 'source', type: 'string' },
          { field: 'action', type: 'string' },
          { field: 'level', type: 'int' },
        ],
      },
    });

    await screen.findByTestId('custom-mapping-row-for-action');

    expect(screen.getAllByTestId(/custom-mapping-row-for-/).map((row) => row.getAttribute('data-testid'))).toEqual([
      'custom-mapping-row-for-action',
      'custom-mapping-row-for-level',
      'custom-mapping-row-for-source',
    ]);
  });

  it('filters the paginated mappings by field name', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(120) } });

    await userEvent.type(await screen.findByLabelText('Search mappings'), 'field_07');

    expect(await screen.findByText('Showing 1-10 of 10 mappings')).toBeInTheDocument();
    expect(screen.getByTestId('custom-mapping-row-for-field_070')).toBeInTheDocument();
    expect(screen.queryByTestId('custom-mapping-row-for-field_000')).not.toBeInTheDocument();
  });

  it('says so when no mapping matches the search', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(120) } });

    await userEvent.type(await screen.findByLabelText('Search mappings'), 'zzz');

    expect(await screen.findByText('No mappings match your search')).toBeInTheDocument();
  });

  it('does not submit the form when pressing enter in the search field', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(120) } });

    await userEvent.type(await screen.findByLabelText('Search mappings'), 'field_07{enter}');

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('does not show a search field for a short list', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(20) } });

    await screen.findByTestId('custom-mapping-row-for-field_019');

    expect(screen.queryByLabelText('Search mappings')).not.toBeInTheDocument();
  });

  it('does not show a pager for a short list', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(20) } });

    await screen.findByTestId('custom-mapping-row-for-field_019');

    expect(screen.queryByRole('button', { name: /next page/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/Showing/)).not.toBeInTheDocument();
  });

  it('adds a new mapping at the top of the first page', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(30) } });

    await userEvent.click(await screen.findByRole('button', { name: /next page/i }));
    await userEvent.click(await screen.findByRole('button', { name: /add mapping/i }));

    expect(await screen.findByText('Showing 1-20 of 31 mappings')).toBeInTheDocument();
    expect(await screen.findByLabelText(/select customFieldMappings\.0\.field/i)).toBeInTheDocument();
  });

  it('points to a mapping with an error on another page', async () => {
    const mappings = buildMappings(60);
    mappings[55] = { field: 'field_055', type: '' };

    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: mappings } });

    await userEvent.type(await screen.findByLabelText(/profile name/i), 'x');

    expect(await screen.findByText(/1 mapping has errors/)).toBeInTheDocument();
    expect(await screen.findByLabelText('Submit')).toBeDisabled();

    await userEvent.click(await screen.findByRole('button', { name: /show first error/i }));

    expect(await screen.findByTestId('custom-mapping-row-for-field_055')).toBeInTheDocument();
  });

  it('submits the mappings of all pages after an edit on a later page', async () => {
    renderProfileForm({ initialValues: { ...profile1, customFieldMappings: buildMappings(60) } });

    await userEvent.click(await screen.findByRole('button', { name: /next page/i }));
    await selectEvent.chooseOption('select customFieldMappings.25.type', 'Number(int)');
    await userEvent.click(await screen.findByLabelText('Submit'));

    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));

    const submittedMappings = mockSubmit.mock.calls[0][0].customFieldMappings;

    expect(submittedMappings).toHaveLength(60);
    expect(submittedMappings[25]).toEqual({ field: 'field_025', type: 'int' });
  });

  it('Do not run onSubmit when has empty name', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        name: '',
      },
    });

    const submitButton = await screen.findByLabelText('Submit');
    await userEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('Do not run onSubmit when has empty customFieldMapping', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        customFieldMappings: [{ field: 'http_method', type: 'string' }],
      },
    });

    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    await userEvent.click(addMappingButton);

    const submitButton = await screen.findByLabelText('Submit');

    await selectEvent.chooseOption('select customFieldMappings.0.type', 'String type');

    await waitFor(async () => {
      expect(screen.queryAllByText('String type')).toHaveLength(2);
    });

    await userEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('Do not run onSubmit when has same fields in customFieldMapping', async () => {
    renderProfileForm({
      initialValues: {
        ...profile1,
        customFieldMappings: [{ field: 'http_method', type: 'string' }],
      },
    });

    const addMappingButton = await screen.findByRole('button', { name: /add mapping/i });

    await userEvent.click(addMappingButton);

    const submitButton = await screen.findByLabelText('Submit');

    await selectEvent.chooseOption('select customFieldMappings.0.type', 'String type');
    await selectEvent.chooseOption('select customFieldMappings.0.field', 'http_method');

    await waitFor(async () => {
      expect(screen.queryAllByText('http_method')).toHaveLength(2);
    });

    await userEvent.click(submitButton);

    expect(mockSubmit).not.toHaveBeenCalled();
  });
});
