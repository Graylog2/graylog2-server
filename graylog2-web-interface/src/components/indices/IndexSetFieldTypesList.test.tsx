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

import { MockStore } from 'helpers/mocking';
import useParams from 'routing/useParams';
import asMock from 'helpers/mocking/AsMock';
import useIndexSetFieldTypes from 'hooks/useIndexSetFieldType';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import type { Attributes } from 'stores/PaginationTypes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypesList';
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';

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
  <TestStoreProvider>
    <IndexSetFieldTypesList />
  </TestStoreProvider>,
);

jest.mock('stores/indices/IndexSetsStore', () => ({
  IndexSetsActions: {
    list: jest.fn(),
  },
  IndexSetsStore: MockStore(['getInitialState', () => ({
    indexSets: [
      { id: '111', title: 'index set title' },
    ],
  })]),
}));

jest.mock('routing/useParams', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes', () => jest.fn());
jest.mock('hooks/useIndexSetFieldType', () => jest.fn());

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

describe('IndexSetFieldTypesList', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useParams).mockImplementation(() => ({
      indexSetId: '111',
    }));

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

  it('Shows modal on edit action click', async () => {
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

  it('Shows modal on remove action click', async () => {
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
    const removeButton = await within(tableRow).findByText('Remove');
    fireEvent.click(removeButton);
    await screen.findByText(/remove custom field type/i);
    const modal = await screen.findByTestId('modal-form');
    await within(modal).findByText('Rotate affected indices after change');

    expect(modal).toHaveTextContent('After removing the custom field type for field in index set title the settings of your search engine will be used');
  });
});
