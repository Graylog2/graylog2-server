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
import userEvent from '@testing-library/user-event';
import { Formik } from 'formik';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import type { LookupTableType } from 'components/lookup-tables/lookup-table-form';

import LookupTableFormFields from './lookup-table-form-fields';

const mockValidateLookupTable = jest.fn();

jest.mock('hooks/useScopePermissions', () => () => ({
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: true },
}));

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  useValidateLookupTable: () => ({ validateLookupTable: mockValidateLookupTable }),
}));

const TABLE_VALUES = {
  title: 'Table',
  description: '',
  name: 'table',
  cache_id: 'cache-id',
  data_adapter_id: 'adapter-id',
  default_single_value: '',
  default_single_value_type: 'NULL',
  default_multi_value: '',
  default_multi_value_type: 'NULL',
} as LookupTableType;

const validationResult = (errors: { [field: string]: Array<string> } = {}) => ({
  failed: Object.keys(errors).length > 0,
  errors,
  error_context: {},
});

const renderWithValues = (values: Partial<LookupTableType>) =>
  render(
    <Formik initialValues={{ ...TABLE_VALUES, ...values }} onSubmit={() => {}}>
      <LookupTableFormFields />
    </Formik>,
  );

describe('LookupTableFormFields', () => {
  beforeEach(() => {
    mockValidateLookupTable.mockResolvedValue(validationResult());
  });

  afterEach(() => {
    mockValidateLookupTable.mockReset();
  });

  it('hides the default value fields when no default type is set', async () => {
    renderWithValues({});

    expect(await screen.findByRole('checkbox', { name: /enable single default value/i })).not.toBeChecked();
    expect(screen.queryByText('Default single value *')).not.toBeInTheDocument();
  });

  it('shows an existing default with an empty value instead of hiding it', async () => {
    renderWithValues({ id: 'table-id', default_single_value: '', default_single_value_type: 'STRING' });

    expect(await screen.findByRole('checkbox', { name: /enable single default value/i })).toBeChecked();
    expect(screen.getByText('Default single value *')).toBeInTheDocument();
  });

  it('keeps the default value field visible after clearing its value', async () => {
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'STRING' });

    await userEvent.clear(await screen.findByDisplayValue('abc'));

    expect(screen.getByRole('checkbox', { name: /enable single default value/i })).toBeChecked();
    expect(screen.getByText('Default single value *')).toBeInTheDocument();
  });

  it('shows the server validation error when leaving the default value field', async () => {
    mockValidateLookupTable.mockResolvedValue(validationResult({ default_single_value: ['Not a number'] }));
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'NUMBER' });

    await userEvent.click(await screen.findByDisplayValue('abc'));
    await userEvent.tab();

    await screen.findByText('Not a number');
    expect(mockValidateLookupTable).toHaveBeenCalledWith(expect.objectContaining({ default_single_value: 'abc' }));
  });

  it('ignores server errors for fields other than the defaults', async () => {
    mockValidateLookupTable.mockResolvedValue(validationResult({ cache_id: ['Cache is required'] }));
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'STRING' });

    await userEvent.click(await screen.findByDisplayValue('abc'));
    await userEvent.tab();

    await waitFor(() => expect(mockValidateLookupTable).toHaveBeenCalled());
    expect(screen.queryByText('Cache is required')).not.toBeInTheDocument();
  });

  it('validates the new type right away when the type changes', async () => {
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'STRING' });

    await userEvent.click(await screen.findByRole('button', { name: /string/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /number/i }));

    await waitFor(() =>
      expect(mockValidateLookupTable).toHaveBeenCalledWith(
        expect.objectContaining({ default_single_value: 'abc', default_single_value_type: 'NUMBER' }),
      ),
    );
  });

  it('clears the server error once the value is edited', async () => {
    mockValidateLookupTable.mockResolvedValue(validationResult({ default_single_value: ['Not a number'] }));
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'NUMBER' });

    const input = await screen.findByDisplayValue('abc');
    await userEvent.click(input);
    await userEvent.tab();
    await screen.findByText('Not a number');

    await userEvent.type(input, '{backspace}');

    expect(screen.queryByText('Not a number')).not.toBeInTheDocument();
  });

  it('drops a validation response that arrives after the value changed', async () => {
    let resolveValidation: (result: ReturnType<typeof validationResult>) => void;
    mockValidateLookupTable.mockReturnValue(
      new Promise((resolve) => {
        resolveValidation = resolve;
      }),
    );
    renderWithValues({ default_single_value: 'abc', default_single_value_type: 'NUMBER' });

    const input = await screen.findByDisplayValue('abc');
    await userEvent.click(input);
    await userEvent.tab();
    await userEvent.type(input, 'd');
    resolveValidation(validationResult({ default_single_value: ['Not a number'] }));

    await screen.findByDisplayValue('abcd');
    await waitFor(() => expect(screen.queryByText('Not a number')).not.toBeInTheDocument());
  });
});
