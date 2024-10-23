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

import FieldType from 'views/logic/fieldtypes/FieldType';
import ChangeFieldType from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useInitialSelection from 'views/logic/fieldactions/ChangeFieldType/hooks/useInitialSelection';
import asMock from 'helpers/mocking/AsMock';
import useFieldTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useInitialSelection', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages', () => jest.fn());
const paginatedFieldUsage = ({
  data: {
    list: [],
    pagination: {
      total: 0,
      page: 1,
      perPage: 1,
      count: 1,
    },
    attributes: [],
  },
  refetch: () => {},
  isInitialLoading: false,
  isLoading: false,
});
const onClose = jest.fn();
const renderChangeTypeAction = ({
  queryId = 'query-id',
  field = 'field',
  type = FieldType.create('STRING'),
  value = 'value',
}) => render(
  <TestStoreProvider>
    <ChangeFieldType onClose={onClose} queryId={queryId} field={field} type={type} value={value} />
  </TestStoreProvider>,
);

describe('ChangeFieldType', () => {
  useViewsPlugin();

  beforeAll(() => {
    asMock(useInitialSelection).mockReturnValue({ list: ['id-1', 'id-2'], isLoading: false });
    asMock(useFieldTypeUsages).mockReturnValue(paginatedFieldUsage);
  });

  it('Shows modal', async () => {
    renderChangeTypeAction({});

    await screen.findByText(/change field field type/i);
  });
});
