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

import asMock from 'helpers/mocking/AsMock';
import FieldType from 'views/logic/fieldtypes/FieldType';
import useFieldTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import useFieldTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFiledTypeUsages';
import type { FieldTypeUsage } from 'views/logic/fieldactions/ChangeFieldType/types';
import ChangeFieldType from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldType';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';

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
const attributes = [
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
const paginatedFieldUsage = (usage: FieldTypeUsage = {
  id: 'id',
  indexSetTitle: 'Index Title',
  streamTitles: ['Stream Title'],
  types: ['string'],
}) => ({
  data: {
    list: [usage],
    pagination: {
      total: 1,
      page: 1,
      perPage: 5,
      count: 1,
    },
    attributes,
  },
  refetch: () => {},
  isInitialLoading: false,
  isFirsLoaded: true,
});

const fieldTypes = {
  data: {
    fieldTypes: {
      string: 'String type',
    },
  },
  isLoading: false,
};
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes', () => jest.fn());

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFiledTypeUsages', () => jest.fn());
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

describe('ChangeFieldType', () => {
  beforeEach(() => {
    asMock(useFieldTypeMutation).mockReturnValue({ isLoading: false, putFiledTypeMutation: () => {} });
    asMock(useFieldTypeUsages).mockReturnValue(paginatedFieldUsage());
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

  it('Shows modal', async () => {
    renderChangeTypeAction({});

    await screen.findByText('Change field field type');
  });
});
