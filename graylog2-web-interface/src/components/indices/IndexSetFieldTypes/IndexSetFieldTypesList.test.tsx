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

import { MockStore } from 'helpers/mocking';
import useParams from 'routing/useParams';
import asMock from 'helpers/mocking/AsMock';
import useIndexSetFieldTypes from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypes/IndexSetFieldTypesList';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import {
  overriddenIndexField,
  defaultField,
  reservedField,
  attributes,
  overriddenProfileField, profileField,
} from 'fixtures/indexSetFieldTypes';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useIndexProfileWithMappingsByField
  from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';

const getData = (list = [defaultField]) => (
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

jest.mock('stores/indices/IndexSetsStore', () => ({
  IndexSetsActions: {
    list: jest.fn(),
  },
  IndexSetsStore: MockStore(['getInitialState', () => ({
    indexSets: [
      { id: '111', title: 'index set title' },
    ],
    indexSet: { id: '111', title: 'index set title', field_type_profile: 'profile-id-111' },
  })]),
}));

jest.mock('routing/useParams', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType', () => jest.fn());

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfile');
jest.mock('components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField');
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions');

jest.mock('use-query-params', () => ({
  ...jest.requireActual('use-query-params'),
  useQueryParam: jest.fn(),
}));

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
          'origin',
          'is_reserved',
          'type'],
      },
      isInitialLoading: false,
    });

    asMock(useFieldTypesForMappings).mockReturnValue({
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

    asMock(useProfile).mockReturnValue({
      data: {
        customFieldMappings: [{ field: 'field-3', type: 'string' }],
        name: 'Profile 1',
        id: 'profile-id-111',
        description: null,
        indexSetIds: [],
      },
      isFetched: true,
      isFetching: false,
      refetch: () => {},
    });

    asMock(useIndexProfileWithMappingsByField).mockReturnValue({
      name: null,
      description: null,
      id: null,
      customFieldMappingsByField: {},
    });

    asMock(useProfileOptions).mockReturnValue({
      options: [
        { value: 'profile-id-111', label: 'Profile 1' },
        { value: 'profile-id-222', label: 'Profile 2' },
      ],
      isLoading: false,
      refetch: () => {},
    });
  });

  describe('Shows list of set field types with correct data', () => {
    it('for field with INDEX origin', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData(),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      await within(tableRow).findByText('field');
      await within(tableRow).findByText('Boolean');
      await within(tableRow).findByText('Index');

      const editButton = await within(tableRow).findByRole('button', { name: /edit/i });

      expect(editButton).not.toBeDisabled();
    });

    it('for field with OVERRIDDEN_INDEX origin', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenIndexField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-1');

      await within(tableRow).findByText('field-1');
      await within(tableRow).findByText('Boolean');
      await within(tableRow).findByText(/overridden index/i);
      const editButton = await within(tableRow).findByRole('button', { name: /edit/i });

      expect(editButton).not.toBeDisabled();
    });

    it('for field with OVERRIDDEN_PROFILE origin', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenProfileField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-2');

      await within(tableRow).findByText('field-2');
      await within(tableRow).findByText('Boolean');
      await within(tableRow).findByText(/overridden profile/i);
      const editButton = await within(tableRow).findByRole('button', { name: /edit/i });

      expect(editButton).not.toBeDisabled();
    });

    it('for field with PROFILE origin', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([profileField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-3');

      await within(tableRow).findByText('field-3');
      await within(tableRow).findByText('String type');
      await within(tableRow).findByText(/profile/i);
      const editButton = await within(tableRow).findByRole('button', { name: /edit/i });

      expect(editButton).not.toBeDisabled();
    });

    it('for field with non reserved type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([defaultField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');

      expect(within(tableRow).queryByTitle('Field has reserved field type')).not.toBeInTheDocument();
    });

    it('for field with reserved type', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([reservedField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-4');

      await within(tableRow).findByTitle('Field has reserved field type');
      const editButton = await within(tableRow).findByRole('button', { name: /edit/i });

      expect(editButton).toBeDisabled();
    });
  });

  describe('Shows modal on reset action click', () => {
    it('for OVERRIDDEN_INDEX origin', async () => {
      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: 'Profile-1',
        description: null,
        id: 'profile-id-111',
        customFieldMappingsByField: { 'field-3': 'String type' },
      });

      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenIndexField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-1');
      const resetButton = await within(tableRow).findByRole('button', { name: /reset/i });
      fireEvent.click(resetButton);
      await screen.findByLabelText(/Remove field type overrides/i);
      const modal = await screen.findByTestId('modal-form');
      await within(modal).findByText('Rotate affected indices after change');

      expect(modal).toHaveTextContent('After removing the overridden field type for field-1 in index set title, the settings of your search engine will be applied for fields: field-1');
    });

    it('for OVERRIDDEN_PROFILE origin', async () => {
      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: 'Profile-1',
        description: null,
        id: 'profile-id-111',
        customFieldMappingsByField: { 'field-2': 'Boolean' },
      });

      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenProfileField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-2');
      const resetButton = await within(tableRow).findByRole('button', { name: /reset/i });
      fireEvent.click(resetButton);
      await screen.findByLabelText(/Remove field type overrides/i);
      const modal = await screen.findByTestId('modal-form');
      await within(modal).findByText('Rotate affected indices after change');

      expect(modal).toHaveTextContent('After removing the overridden field type for field-2 in index set title, the settings from Profile-1 ( namely field-2: Boolean) will be applied');
    });
  });

  describe('Shows expanded row on origin bage click', () => {
    it('for origin index', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([defaultField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field');
      const originBadge = await within(tableRow).findByText(/index/i);
      fireEvent.click(originBadge);

      expect(tableRow).toHaveTextContent('Field type Boolean comes from the search engine index mapping. It could have been created dynamically, set by Graylog instance or come from historical profiles and/or custom mappings.');
    });

    it('for origin profile', async () => {
      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: 'Profile-1',
        description: null,
        id: 'profile-id-111',
        customFieldMappingsByField: { 'field-3': 'String' },
      });

      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([profileField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-3');
      const originBadge = await within(tableRow).findByText(/profile/i);
      fireEvent.click(originBadge);

      expect(tableRow).toHaveTextContent('Field type String type comes from profile Profile-1. It overrides possible mappings from the search engine index mapping, either immediately (if index was rotated) or during the next rotation');
    });

    it('for origin overridden index', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenIndexField]),
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-1');
      const originBadge = await within(tableRow).findByText(/overridden index/i);
      fireEvent.click(originBadge);

      expect(tableRow).toHaveTextContent('Field type Boolean comes from the individual, custom field type mapping. It overrides possible mappings from the search engine index mapping, either immediately (if index was rotated) or during the next rotation.');
    });

    it('for origin overridden profile', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([overriddenProfileField]),
      });

      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: 'Profile-1',
        description: null,
        id: 'profile-id-111',
        customFieldMappingsByField: { 'field-2': 'Boolean' },
      });

      renderIndexSetFieldTypesList();
      const tableRow = await screen.findByTestId('table-row-field-2');
      const originBadge = await within(tableRow).findByText(/overridden profile/i);
      fireEvent.click(originBadge);

      expect(tableRow).toHaveTextContent('Field type Boolean comes from the individual, custom field type mapping. It overrides not only possible mappings from the search engine index mapping, but also mapping field-2: Boolean present in profile Profile-1');
    });
  });

  describe('Index filed type profile', () => {
    it('shown profile name when index set has profile', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([defaultField]),
      });

      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: 'Profile-1',
        description: 'Some profile description',
        id: 'profile-id-111',
        customFieldMappingsByField: { 'field-2': 'Boolean' },
      });

      renderIndexSetFieldTypesList();
      const row = await screen.findByTitle('Some profile description');

      expect(row).toHaveTextContent('Field type mapping profile:Profile-1');
    });

    it('shown Not set when index set has no profile', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([defaultField]),
      });

      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: null,
        description: null,
        id: null,
        customFieldMappingsByField: null,
      });

      renderIndexSetFieldTypesList();
      const row = await screen.findByTitle('Field type mapping profile not set yet');

      expect(row).toHaveTextContent('Field type mapping profile:Not set');
    });

    it('shows set profile modal on button click', async () => {
      asMock(useIndexSetFieldTypes).mockReturnValue({
        isLoading: false,
        refetch: () => {},
        data: getData([defaultField]),
      });

      asMock(useIndexProfileWithMappingsByField).mockReturnValue({
        name: null,
        description: null,
        id: null,
        customFieldMappingsByField: null,
      });

      renderIndexSetFieldTypesList();
      const button = await screen.findByTitle('Set field type profile');
      fireEvent.click(button);
      const modal = await screen.findByTestId('modal-form');
      await within(modal).findByRole('button', { name: /Set Profile/i, hidden: true });
    });
  });
});
