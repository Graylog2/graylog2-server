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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import asMock from 'helpers/mocking/AsMock';
import useFieldTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';
import useFieldTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import type { Attributes } from 'stores/PaginationTypes';
import suppressConsole from 'helpers/suppressConsole';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';

const onCloseMock = jest.fn();
const renderChangeFieldTypeModal = ({
  onClose = onCloseMock,
  field = 'field',
  show = true,
  showSelectionTable = undefined,
  initialFieldType = undefined,
  initialSelectedIndexSets = ['id-1', 'id-2'],
}) => render(
  <TestStoreProvider>
    <ChangeFieldTypeModal onClose={onClose}
                          initialData={{
                            fieldName: field,
                            type: initialFieldType,
                          }}
                          show={show}
                          initialSelectedIndexSets={initialSelectedIndexSets}
                          showSelectionTable={showSelectionTable} />
  </TestStoreProvider>,
);
const attributes: Attributes = [
  {
    id: 'index_set_id',
    title: 'Index Set Id',
    type: 'STRING',
    sortable: true,
    hidden: true,
  },
  {
    id: 'index_set_title',
    title: 'Index Set Title',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'stream_titles',
    title: 'Stream Titles',
    type: 'STRING',
    sortable: false,
  },
  {
    id: 'types',
    title: 'Field Type History',
    type: 'STRING',
    sortable: false,
  },
];

const fieldTypeUsages = [
  {
    id: 'id-1',
    indexSetTitle: 'Index Title 1',
    streamTitles: ['Stream Title 1'],
    types: ['string'],
  },
  {
    id: 'id-2',
    indexSetTitle: 'Index Title 2',
    streamTitles: ['Stream Title 2'],
    types: ['int'],
  },
];
const paginatedFieldUsage = ({
  data: {
    list: fieldTypeUsages,
    pagination: {
      total: 2,
      page: 1,
      perPage: 5,
      count: 1,
    },
    attributes,
  },
  refetch: () => {
  },
  isInitialLoading: false,
  isLoading: false,
});

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation', () => jest.fn());

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

describe('ChangeFieldTypeModal', () => {
  const putFieldTypeMutationMock = jest.fn(() => Promise.resolve());

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useFieldTypesForMappings).mockReturnValue({
      isLoading: false,
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
        },
      },
    });

    asMock(useFieldTypeMutation).mockReturnValue({ isLoading: false, putFieldTypeMutation: putFieldTypeMutationMock });
    asMock(useFieldTypeUsages).mockReturnValue(paginatedFieldUsage);

    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        displayedAttributes: ['index_set_title',
          'stream_titles',
          'types'],
      },
      isInitialLoading: false,
    });
  });

  it('Shows rotating indexes', async () => {
    renderChangeFieldTypeModal({});

    await screen.findByText('Rotate affected indices after change');
  });

  it('Shows type options', async () => {
    renderChangeFieldTypeModal({});

    await suppressConsole(async () => {
      const typeSelect = await screen.findByLabelText(/select field type for field/i);
      selectEvent.openMenu(typeSelect);
    });

    await screen.findByText('Boolean');
  });

  it('Shows index sets data', async () => {
    renderChangeFieldTypeModal({});

    await screen.findByText('Stream Title 1');
    await screen.findByText('Index Title 1');
    await screen.findByText('String type');
    await screen.findByText('Stream Title 2');
    await screen.findByText('Index Title 2');
    await screen.findByText('Number(int)');
  });

  it('run putFieldTypeMutationMock with selected type and indexes', async () => {
    renderChangeFieldTypeModal({});

    const typeSelect = await screen.findByLabelText(/select field type for field/i);
    selectEvent.openMenu(typeSelect);
    await selectEvent.select(typeSelect, 'Number(int)');

    const submit = await screen.findByTitle(/change field type/i);

    const rowCheckboxes = await screen.findAllByTitle(/deselect entity/i);
    fireEvent.click(rowCheckboxes[1]);
    fireEvent.click(submit);

    await waitFor(() => expect(putFieldTypeMutationMock).toHaveBeenCalledWith({
      indexSetSelection: ['id-1'],
      newFieldType: 'int',
      rotated: true,
      field: 'field',
    }));
  });

  it('run putFieldTypeMutationMock with selected type and indexes when showSelectionTable false', async () => {
    renderChangeFieldTypeModal({ initialSelectedIndexSets: ['id-2'] });

    const typeSelect = await screen.findByLabelText(/select field type for field/i);
    selectEvent.openMenu(typeSelect);
    await selectEvent.select(typeSelect, 'Number(int)');

    const submit = await screen.findByTitle(/change field type/i);

    fireEvent.click(submit);

    await waitFor(() => expect(putFieldTypeMutationMock).toHaveBeenCalledWith({
      indexSetSelection: ['id-2'],
      newFieldType: 'int',
      rotated: true,
      field: 'field',
    }));
  });

  it('Doesn\'t shows index sets data when showSelectionTable false', async () => {
    renderChangeFieldTypeModal({ showSelectionTable: false });

    expect(screen.queryByText('Stream Title 1')).not.toBeInTheDocument();
    expect(screen.queryByText('Stream Title 1')).not.toBeInTheDocument();
    expect(screen.queryByText('Index Title 1')).not.toBeInTheDocument();
    expect(screen.queryByText('String type')).not.toBeInTheDocument();
    expect(screen.queryByText('Stream Title 2')).not.toBeInTheDocument();
    expect(screen.queryByText('Index Title 2')).not.toBeInTheDocument();
    expect(screen.queryByText('Number(int)')).not.toBeInTheDocument();
  });

  it('Use initial type', async () => {
    renderChangeFieldTypeModal({ initialFieldType: 'bool' });

    await screen.findByText('Boolean');
  });
});
