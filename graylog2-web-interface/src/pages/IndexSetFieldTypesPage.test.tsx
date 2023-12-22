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
import { render, screen, fireEvent, within } from 'wrappedTestingLibrary';
import { useQueryParam, QueryParamProvider } from 'use-query-params';
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';

import asMock from 'helpers/mocking/AsMock';
import useIndexSetFieldTypes from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import IndexSetFieldTypesPage from 'pages/IndexSetFieldTypesPage';
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import { customField, defaultField, attributes } from 'fixtures/indexSetFieldTypes';

const getData = (list = [defaultField]) => (
  {
    list,
    pagination: {
      total: 1,
    },
    attributes,
  }
);

const renderIndexSetFieldTypesPage = () => render(
  <QueryParamProvider adapter={ReactRouter6Adapter}>
    <TestStoreProvider>
      <IndexSetFieldTypesPage />
    </TestStoreProvider>,
  </QueryParamProvider>,
);

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType', () => jest.fn());

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

jest.mock('use-query-params', () => ({
  ...jest.requireActual('use-query-params'),
  useQueryParam: jest.fn(),
}));

describe('IndexSetFieldTypesList', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        displayedAttributes: ['field_name',
          'is_custom',
          'is_reserved',
          'type'],
      },
      isInitialLoading: false,
    });

    asMock(useFieldTypes).mockReturnValue({
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
        },
      },
      isLoading: false,
    });

    asMock(useQueryParam).mockImplementation(() => ([undefined, () => {}]));
  });

  it('Shows modal on edit click', async () => {
    asMock(useIndexSetFieldTypes).mockReturnValue({
      isLoading: false,
      refetch: () => {},
      data: getData([customField]),

    });

    renderIndexSetFieldTypesPage();
    const tableRow = await screen.findByTestId('table-row-field');
    const editButton = await within(tableRow).findByText('Edit');
    fireEvent.click(editButton);
    await screen.findByText(/change field field type/i);
    const modal = await screen.findByTestId('modal-form');
    await within(modal).findByText('Boolean');

    expect(within(modal).queryByText(/select targeted index sets/i)).not.toBeInTheDocument();
  });

  it('Shows modal on Change field type click', async () => {
    asMock(useIndexSetFieldTypes).mockReturnValue({
      isLoading: false,
      refetch: () => {},
      data: getData([customField]),
    });

    renderIndexSetFieldTypesPage();
    const editButton = await screen.findByText(/change field type/i);
    fireEvent.click(editButton);

    const modal = await screen.findByTestId('modal-form');
    await within(modal).findByText(/change field type/i);
    await within(modal).findByText(/select or type the field/i);

    expect(within(modal).queryByText(/select targeted index sets/i)).not.toBeInTheDocument();
  });
});
