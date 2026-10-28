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
import { Formik } from 'formik';

import type { LookupTable } from 'logic/lookup-tables/types';
import { UNSUPPORTED_PREVIEW } from 'components/lookup-tables/fixtures';

import SummaryStep from './summary-step';

const mockErrorsConsumer = jest.fn((_props: unknown) => null);
const mockValidateLookupTable = jest.fn();

jest.mock('components/lookup-tables/lookup-table-list/errors-consumer', () => (props: unknown) => {
  mockErrorsConsumer(props);

  return null;
});

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  useFetchCache: () => ({ cache: undefined, loadingCache: false }),
  useFetchDataAdapter: () => ({ dataAdapter: undefined, loadingDataAdapter: false }),
  usePurgeAllLookupTableKey: () => ({ purgeAllLookupTableKey: jest.fn() }),
  usePurgeLookupTableKey: () => ({ purgeLookupTableKey: jest.fn() }),
  useFetchLookupPreview: () => ({ lookupPreview: UNSUPPORTED_PREVIEW }),
  useValidateLookupTable: () => ({ validateLookupTable: mockValidateLookupTable }),
  useTestLookupTableKey: () => ({ testLookupTableKey: jest.fn() }),
}));

const NEW_TABLE_VALUES = {
  id: undefined,
  title: 'New table',
  description: '',
  name: 'new-table-name',
  cache_id: undefined,
  data_adapter_id: undefined,
  default_single_value: '',
  default_single_value_type: 'NULL',
  default_multi_value: '',
  default_multi_value_type: 'NULL',
  content_pack: null,
} as unknown as LookupTable;

const validationResult = (errors: { [field: string]: Array<string> } = {}) => ({
  failed: Object.keys(errors).length > 0,
  errors,
  error_context: {},
});

function renderWithValues(values: LookupTable) {
  return render(
    <Formik initialValues={values} onSubmit={() => {}}>
      <SummaryStep />
    </Formik>,
  );
}

describe('SummaryStep', () => {
  beforeEach(() => {
    mockValidateLookupTable.mockResolvedValue(validationResult());
  });

  afterEach(() => {
    mockErrorsConsumer.mockClear();
    mockValidateLookupTable.mockReset();
  });

  it('shows the invalid default values the server reports', async () => {
    mockValidateLookupTable.mockResolvedValue(
      validationResult({ default_multi_value: ['Could not parse JSON object value <hello>'], name: ['Name in use'] }),
    );
    renderWithValues({ ...NEW_TABLE_VALUES, default_multi_value: 'hello', default_multi_value_type: 'OBJECT' });

    await screen.findByText(/invalid default values/i);
    expect(screen.getByText(/default multi value: could not parse json object value <hello>/i)).toBeInTheDocument();
    expect(screen.queryByText(/default single value:/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/name in use/i)).not.toBeInTheDocument();
  });

  it('does not show a default value alert when the server accepts the defaults', async () => {
    renderWithValues(NEW_TABLE_VALUES);

    await waitFor(() => expect(mockValidateLookupTable).toHaveBeenCalled());
    await screen.findByText(/lookup table details/i);
    expect(screen.queryByText(/invalid default values/i)).not.toBeInTheDocument();
  });
});
