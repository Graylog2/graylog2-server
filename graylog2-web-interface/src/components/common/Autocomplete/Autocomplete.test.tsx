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
import { Formik } from 'formik';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import Autocomplete from './Autocomplete';

const OPTIONS = [
  { value: 'Rojo', label: 'Red' },
  { value: 'Verde', label: 'Green' },
  { value: 'Amarillo', label: 'Yellow' },
];

const renderAutocomplete = () =>
  render(
    <Formik initialValues={{ value: 'Verde', label: 'Green' }} onSubmit={() => null}>
      <Autocomplete fieldName="spaColor" label="Color translator" helpText="Choose a color" options={OPTIONS} />
    </Formik>,
  );

describe('Autocomplete component', () => {
  it('should render the field with a label', async () => {
    renderAutocomplete();

    const label = await screen.findByText('Color translator');
    const pseudoInput = screen.getByRole('combobox');

    expect(label).toBeVisible();
    expect(pseudoInput).toBeVisible();
  });

  it('should let the user type', async () => {
    renderAutocomplete();
    const input = screen.getByRole('combobox');

    await userEvent.type(input, 'Naranja');

    expect(input).toHaveValue('Naranja');
  });

  it('should show a list with options', async () => {
    renderAutocomplete();
    const input = screen.getByRole('combobox');

    await userEvent.type(input, 'ver');

    await waitFor(() => {
      expect(screen.getByRole('listbox')).toBeVisible();
    });
  });
});
