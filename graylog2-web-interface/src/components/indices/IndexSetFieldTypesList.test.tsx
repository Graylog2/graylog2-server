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
import useIndexSetFieldTypes from 'hooks/useIndexSetFieldType';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import type { Attributes } from 'stores/PaginationTypes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypesList';
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';

const attributes: Attributes = [
  {
    id: 'field_name',
    title: 'Field Name',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'is_custom',
    title: 'Custom',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'is_reserved',
    title: 'Reserved',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'type',
    title: 'Type',
    type: 'STRING',
    sortable: true,
  },
];

const getData = (list = [{
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: false,
  isReserved: false,
}]) => (
  {
    list,
    pagination: {
      total: 1,
    },
    attributes,
  }
);
const renderIndexSetFieldTypesList = () => render(
  <QueryParamProvider adapter={ReactRouter6Adapter}>
    <TestStoreProvider>
      <IndexSetFieldTypesList />
    </TestStoreProvider>,
  </QueryParamProvider>,
);

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes', () => jest.fn());
jest.mock('hooks/useIndexSetFieldType', () => jest.fn());

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

    asMock(useFiledTypes).mockReturnValue({
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

  describe('Shows list of set field types with correct data', () => {
    it('for field with non custom type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData(),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      await within(tableRow).findByText('field');
      await within(tableRow).findByText('Boolean');
      await within(tableRow).findByText('Edit');

      expect(within(tableRow).queryByTitle('Field has custom field type')).not.toBeInTheDocument();
    });

    it('for field with custom type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([{
          id: 'field',
          fieldName: 'field',
          type: 'bool',
          isCustom: true,
          isReserved: false,
        }]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      await within(tableRow).findByText('field');
      await within(tableRow).findByText('Boolean');
      await within(tableRow).findByText('Edit');
      await within(tableRow).findByTitle('Field has custom field type');
    });

    it('for field with non reserved type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([{
          id: 'field',
          fieldName: 'field',
          type: 'bool',
          isCustom: true,
          isReserved: false,
        }]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      const editButton = await within(tableRow).findByText('Edit');

      expect(within(tableRow).queryByTitle('Field has reserved field type')).not.toBeInTheDocument();
      expect(editButton.hasAttribute('disabled')).toBe(false);
    });

    it('for field with reserved type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([{
          id: 'field',
          fieldName: 'field',
          type: 'bool',
          isCustom: true,
          isReserved: true,
        }]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      const editButton = await within(tableRow).findByText('Edit');
      await within(tableRow).findByTitle('Field has reserved field type');

      expect(editButton.hasAttribute('disabled')).toBe(true);
    });
  });

  it('Shows modal on action click', async () => {
    asMock(useIndexSetFieldTypes).mockReturnValue({
      isLoading: false,
      refetch: () => {},
      data: getData([{
        id: 'field',
        fieldName: 'field',
        type: 'bool',
        isCustom: true,
        isReserved: false,
      }]),
    });

    renderIndexSetFieldTypesList();
    const tableRow = await screen.findByTestId('table-row-field');
    const editButton = await within(tableRow).findByText('Edit');
    fireEvent.click(editButton);
    await screen.findByText(/change field field type/i);
    const modal = await screen.findByTestId('modal-form');
    await within(modal).findByText('Boolean');

    expect(within(modal).queryByText(/select targeted index sets/i)).not.toBeInTheDocument();
  });
});
