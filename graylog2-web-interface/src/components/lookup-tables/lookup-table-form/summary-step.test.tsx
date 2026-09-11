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
import { render } from 'wrappedTestingLibrary';
import { Formik } from 'formik';

import type { LookupTable } from 'logic/lookup-tables/types';
import { UNSUPPORTED_PREVIEW } from 'components/lookup-tables/fixtures';

import SummaryStep from './summary-step';

const mockErrorsConsumer = jest.fn((_props: unknown) => null);

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

function renderWithValues(values: LookupTable) {
  return render(
    <Formik initialValues={values} onSubmit={() => {}}>
      <SummaryStep />
    </Formik>,
  );
}

describe('SummaryStep', () => {
  afterEach(() => {
    mockErrorsConsumer.mockClear();
  });

  it('does not poll for lookup table errors while creating a new table that has not been saved yet', () => {
    renderWithValues(NEW_TABLE_VALUES);

    expect(mockErrorsConsumer).toHaveBeenCalledWith(expect.objectContaining({ lutNames: undefined }));
  });

  it('polls for lookup table errors once the table has been saved and has an id', () => {
    renderWithValues({ ...NEW_TABLE_VALUES, id: 'existing-table-id' });

    expect(mockErrorsConsumer).toHaveBeenCalledWith(expect.objectContaining({ lutNames: ['new-table-name'] }));
  });
});
