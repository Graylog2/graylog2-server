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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import { MockStore } from 'helpers/mocking';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useIndexProfileWithMappingsByField
  from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';

jest.mock('stores/indices/IndexSetsStore', () => ({
  IndexSetsActions: {
    list: jest.fn(),
  },
  IndexSetsStore: MockStore(['getInitialState', () => ({
    indexSets: [
      { id: '111', title: 'index set title', field_type_profile: 'profile-id-111' },
    ],
    indexSet: { id: '111', title: 'index set title', field_type_profile: 'profile-id-111' },
  })]),
}));

jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfile', () => jest.fn());
const renderUseIndexProfileWithMappingsByField = () => renderHook(() => useIndexProfileWithMappingsByField());

describe('useRemoveCustomFieldTypeMutation', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useFieldTypesForMappings).mockReturnValue({
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
          ip: 'IP',
          date: 'Date',
        },
      },
      isLoading: false,
    });

    asMock(useProfile).mockReturnValue({
      isFetched: true,
      isFetching: false,
      refetch: () => {},
      data: {
        name: 'Profile 1',
        id: 'profile-id-111',
        description: 'Profile description',
        indexSetIds: [],
        customFieldMappings: [{
          field: 'field-1',
          type: 'ip',
        }, {
          field: 'field-2',
          type: 'int',
        }],
      },
    });
  });

  it('return correct data', async () => {
    const { result } = renderUseIndexProfileWithMappingsByField();

    expect(result.current).toEqual({
      customFieldMappingsByField: {
        'field-1': 'IP',
        'field-2': 'Number(int)',
      },
      name: 'Profile 1',
      description: 'Profile description',
      id: 'profile-id-111',
    });
  });
});
