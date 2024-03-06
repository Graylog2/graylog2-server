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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import IndexSetCustomFieldTypeRemoveModal from 'components/indices/IndexSetFieldTypes/IndexSetCustomFieldTypeRemoveModal';
import useRemoveCustomFieldTypeMutation from 'components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useIndexProfileWithMappingsByField
  from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';

const mockOnClosed = jest.fn();
const renderIndexSetCustomFieldTypeRemoveModal = () => render(
  <TestStoreProvider>
    <IndexSetCustomFieldTypeRemoveModal indexSetIds={['111']} show onClose={mockOnClosed} fields={['field']} />
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

jest.mock('components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation', () => jest.fn());
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');
jest.mock('components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField');

describe('IndexSetFieldTypesList', () => {
  const mockedRemoveCustomFieldTypeMutation = jest.fn(() => Promise.resolve());

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: ['field'],
      setSelectedEntities: () => {},
      selectEntity: () => {},
      deselectEntity: () => {},
    });

    asMock(useRemoveCustomFieldTypeMutation).mockReturnValue({
      removeCustomFieldTypeMutation: mockedRemoveCustomFieldTypeMutation,
      isLoading: false,
    });

    asMock(useIndexProfileWithMappingsByField).mockReturnValue({
      name: null,
      description: null,
      id: null,
      customFieldMappingsByField: {},
    });
  });

  describe('IndexSetCustomFieldTypeRemoveModal', () => {
    it('Runs mockedRemoveCustomFieldTypeMutation on submit with rotation', async () => {
      renderIndexSetCustomFieldTypeRemoveModal();

      const submit = await screen.findByRole('button', {
        name: /remove field type overrides/i,
        hidden: true,
      });
      fireEvent.click(submit);

      expect(mockedRemoveCustomFieldTypeMutation).toHaveBeenCalledWith({
        fields: ['field'],
        indexSets: ['111'],
        rotated: true,
      });
    });

    it('Runs mockedRemoveCustomFieldTypeMutation on submit without rotation', async () => {
      renderIndexSetCustomFieldTypeRemoveModal();

      const checkbox = await screen.findByText(/rotate affected indices after change/i);
      const submit = await screen.findByRole('button', {
        name: /remove field type overrides/i,
        hidden: true,
      });
      fireEvent.click(checkbox);
      fireEvent.click(submit);

      expect(mockedRemoveCustomFieldTypeMutation).toHaveBeenCalledWith({
        fields: ['field'],
        indexSets: ['111'],
        rotated: false,
      });
    });
  });
});
