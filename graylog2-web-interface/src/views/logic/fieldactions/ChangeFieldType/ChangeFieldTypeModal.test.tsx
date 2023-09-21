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
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import useFieldTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import type { Attributes } from 'stores/PaginationTypes';

const onCloseMock = jest.fn();
const renderChangeFieldTypeModal = ({
  onClose = onCloseMock,
  field = 'field',
  show = true,
}) => render(
  <TestStoreProvider>
    <ChangeFieldTypeModal onClose={onClose} field={field} show={show} />
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
  isFirsLoaded: true,
});

const fieldTypes: {
  data: { fieldTypes: FieldTypes },
  isLoading: boolean,
} = {
  data: {
    fieldTypes: {
      string: 'String type',
      int: 'Number(int)',
    },
  },
  isLoading: false,
};
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes', () => jest.fn());

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFiledTypeUsages', () => jest.fn());
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

describe('ChangeFieldTypeModal', () => {
  const putFiledTypeMutationMock = jest.fn(() => Promise.resolve());

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useFieldTypeMutation).mockReturnValue({ isLoading: false, putFiledTypeMutation: putFiledTypeMutationMock });
    asMock(useFieldTypeUsages).mockReturnValue(paginatedFieldUsage);
    asMock(useFieldTypes).mockReturnValue(fieldTypes);

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

    await screen.findByText('Rotating indexes');
  });

  it('Shows type options', async () => {
    renderChangeFieldTypeModal({});

    const typeSelect = await screen.findByText(/select field type/i);
    selectEvent.openMenu(typeSelect);

    await screen.findByText('String type');
    await screen.findByText('Number(int)');
  });

  it('Shows Show index sets button without indexes', async () => {
    renderChangeFieldTypeModal({});

    await screen.findByText('Show index sets');

    expect(screen.queryByText('Index Title')).not.toBeInTheDocument();
  });

  it('Shows index sets data on Show index sets button click', async () => {
    renderChangeFieldTypeModal({});

    const showIndexSetsButton = await screen.findByText('Show index sets');
    fireEvent.click(showIndexSetsButton);

    await screen.findByText('Stream Title 1');
    await screen.findByText('Index Title 1');
    await screen.findByText('String type');
    await screen.findByText('Stream Title 2');
    await screen.findByText('Index Title 2');
    await screen.findByText('Number(int)');
  });

  it('run putFiledTypeMutationMock with selected type and indexes', async () => {
    renderChangeFieldTypeModal({});

    const typeSelect = await screen.findByLabelText(/select field type/i);
    selectEvent.openMenu(typeSelect);
    await selectEvent.select(typeSelect, 'Number(int)');

    await screen.findByText('Number(int)');
    const submit = await screen.findByTitle(/change field type/i);
    fireEvent.click(submit);

    const showIndexSetsButton = await screen.findByText('Show index sets');
    fireEvent.click(showIndexSetsButton);

    const rowCheckboxes = await screen.findAllByTitle(/deselect entity/i);
    fireEvent.click(rowCheckboxes[1]);

    await waitFor(() => expect(putFiledTypeMutationMock).toHaveBeenCalledWith({
      indexSetSelection: ['id-1', 'id-2'],
      newFieldType: 'int',
      rotated: false,
      field: 'field',
    }));
  });
});
