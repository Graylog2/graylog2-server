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

import { simpleFields } from 'fixtures/fields';
import { asMock } from 'helpers/mocking';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import FieldsOverview from './FieldsOverview';

jest.mock('views/hooks/useActiveQueryId');

const fields = simpleFields();
const fieldTypes = {
  all: fields,
  currentQuery: fields.shift(),
};

const searchFor = async (term: string) => {
  const searchInput = await screen.findByPlaceholderText('Filter fields');
  await userEvent.type(searchInput, term);
};

describe('<FieldsOverview />', () => {
  const SimpleFieldsOverview = () => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <FieldsOverview />
    </FieldTypesContext.Provider>
  );

  beforeEach(() => {
    asMock(useActiveQueryId).mockReturnValue('aQueryId');
  });

  it('should render a FieldsOverview', async () => {
    render(<SimpleFieldsOverview />);

    await screen.findByText('http_method');
  });

  it('should render all fields in FieldsOverview after click', async () => {
    render(<SimpleFieldsOverview />);

    const allFields = await screen.findByRole('button', { name: /this shows all fields, but no reserved/i });

    expect(screen.queryByText('date')).not.toBeInTheDocument();

    userEvent.click(allFields);

    await screen.findByText('date');
    await screen.findByText('http_method');

    const currentQuery = await screen.findByRole('button', { name: /fields which occur in your current query/i });
    userEvent.click(currentQuery);

    await waitFor(() => {
      expect(screen.queryByText('date')).not.toBeInTheDocument();
    });
    await screen.findByText('http_method');
  });

  it('should search in the field list', async () => {
    render(<SimpleFieldsOverview />);

    await searchFor('http');
    const allFields = await screen.findByRole('button', { name: /this shows all fields, but no reserved/i });
    userEvent.click(allFields);

    await screen.findByText('http_method');
    await waitFor(() => {
      expect(screen.queryByText('date')).not.toBeInTheDocument();
    });
  });

  it('should show hint when field types are `undefined`', async () => {
    render(<FieldsOverview />);
    await screen.findByText(/no field information available/i);
  });

  it('should show hint when no fields are returned after filtering', async () => {
    render(<SimpleFieldsOverview />);

    await searchFor('non_existing_field');
    await screen.findByText(/no fields to show/i);
    await screen.findByText(/Try changing your filter term/i);
  });
});
