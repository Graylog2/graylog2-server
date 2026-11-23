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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import selectEvent from 'helpers/selectEvent';
import Routes from 'routing/Routes';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFetchIndexSetFieldTypes from 'components/indices/IndexSetFieldTypeProfiles/hooks/useFetchIndexSetFieldTypes';
import ImportProfileModal from 'components/indices/IndexSetFieldTypeProfiles/ImportProfileModal';
import type { IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/types';

const mockPushWithState = jest.fn();

jest.mock('routing/useHistory', () => {
  const actual = jest.requireActual('routing/useHistory');

  return {
    __esModule: true,
    default: () => ({ ...actual.default(), pushWithState: mockPushWithState }),
  };
});

jest.mock('components/indices/hooks/useIndexSetsList', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useFetchIndexSetFieldTypes', () => jest.fn());

const fields: Array<IndexSetFieldTypeJson> = [
  { field_name: 'from_profile', type: 'string', origin: 'PROFILE', is_reserved: false },
  { field_name: 'from_override', type: 'long', origin: 'OVERRIDDEN_INDEX', is_reserved: false },
  { field_name: 'from_index', type: 'ip', origin: 'INDEX', is_reserved: false },
  { field_name: 'reserved_field', type: 'string', origin: 'INDEX', is_reserved: true },
  { field_name: 'unknown_type', type: 'nested', origin: 'INDEX', is_reserved: false },
];

describe('ImportProfileModal', () => {
  const fetchFieldTypes = jest.fn();
  const reset = jest.fn();

  const mockFetch = (result: Array<IndexSetFieldTypeJson>) =>
    fetchFieldTypes.mockImplementation((_indexSetId, { onSuccess }) => onSuccess(result));

  const renderModal = () => render(<ImportProfileModal onClose={jest.fn()} />);

  const importFrom = async () => {
    await selectEvent.chooseOption('Index set', 'Index set 1');
    await userEvent.click(await screen.findByRole('button', { name: /^import$/i }));
  };

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useIndexSetsList).mockReturnValue({
      data: { indexSets: [{ id: 'set-1', title: 'Index set 1' }], indexSetsCount: 1, indexSetStats: null },
      refetch: jest.fn(),
      isSuccess: true,
      isInitialLoading: false,
    } as any);

    asMock(useFieldTypesForMappings).mockReturnValue({
      data: { fieldTypes: { string: 'String', long: 'Number(long)', ip: 'IP' } },
      isLoading: false,
    });

    asMock(useFetchIndexSetFieldTypes).mockReturnValue({
      fetchFieldTypes,
      isLoading: false,
      isError: false,
      reset,
    });
  });

  it('starts with all origins ticked and disables Import until an index set is chosen', async () => {
    renderModal();

    expect(await screen.findByRole('checkbox', { name: 'Field Type Profile' })).toBeChecked();
    expect(await screen.findByRole('checkbox', { name: 'Index Field Configuration' })).toBeChecked();
    expect(await screen.findByRole('checkbox', { name: 'Index' })).toBeChecked();
    expect(await screen.findByRole('button', { name: /^import$/i })).toBeDisabled();
  });

  it('disables Import when no origin is ticked', async () => {
    renderModal();

    await selectEvent.chooseOption('Index set', 'Index set 1');
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Field Type Profile' }));
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Index Field Configuration' }));
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Index' }));

    expect(await screen.findByRole('button', { name: /^import$/i })).toBeDisabled();
  });

  it('opens the create form with the importable fields and the count of left out fields', async () => {
    mockFetch(fields);
    renderModal();

    await importFrom();

    expect(mockPushWithState).toHaveBeenCalledWith(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE, {
      customFieldMappings: [
        { field: 'from_profile', type: 'string' },
        { field: 'from_override', type: 'long' },
        { field: 'from_index', type: 'ip' },
      ],
      skippedFieldsCount: 2,
    });
  });

  it('leaves out fields with a reserved name even when the index set does not flag them', async () => {
    mockFetch([fields[0], { field_name: 'message', type: 'string', origin: 'INDEX', is_reserved: false }]);
    renderModal();

    await importFrom();

    expect(mockPushWithState).toHaveBeenCalledWith(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE, {
      customFieldMappings: [{ field: 'from_profile', type: 'string' }],
      skippedFieldsCount: 1,
    });
  });

  it('imports only fields with a custom mapping when only Index Field Configuration is ticked', async () => {
    mockFetch(fields);
    renderModal();

    await selectEvent.chooseOption('Index set', 'Index set 1');
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Field Type Profile' }));
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Index' }));
    await userEvent.click(await screen.findByRole('button', { name: /^import$/i }));

    expect(mockPushWithState).toHaveBeenCalledWith(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE, {
      customFieldMappings: [{ field: 'from_override', type: 'long' }],
      skippedFieldsCount: 0,
    });
  });

  it('stays open with a note when there is nothing to import', async () => {
    mockFetch([fields[3]]);
    renderModal();

    await importFrom();

    await screen.findByText('No fields found for the selected origins');

    expect(mockPushWithState).not.toHaveBeenCalled();
  });

  it('shows an inline error with Retry when loading the fields failed', async () => {
    asMock(useFetchIndexSetFieldTypes).mockReturnValue({
      fetchFieldTypes,
      isLoading: false,
      isError: true,
      reset,
    });

    renderModal();

    await selectEvent.chooseOption('Index set', 'Index set 1');
    await userEvent.click(await screen.findByRole('button', { name: /retry/i }));

    expect(fetchFieldTypes).toHaveBeenCalledWith('set-1', expect.anything());
  });
});
