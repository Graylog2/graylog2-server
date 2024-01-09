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
import { QueryParamProvider } from 'use-query-params';
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';

import asMock from 'helpers/mocking/AsMock';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { profile1, attributes, profile2 } from 'fixtures/indexSetFieldTypeProfiles';
import ProfilesList from 'components/indices/IndexSetFieldTypeProfiles/ProfilesList';
import useProfiles from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfiles';

const getData = (list = [profile1]) => (
  {
    list,
    pagination: {
      total: 1,
    },
    attributes,
  }
);

const renderIndexSetFieldTypeProfilesList = () => render(
  <QueryParamProvider adapter={ReactRouter6Adapter}>
    <TestStoreProvider>
      <ProfilesList />
    </TestStoreProvider>,
  </QueryParamProvider>,
);

jest.mock('routing/useParams', () => jest.fn());

jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfiles', () => jest.fn());
jest.mock('views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings', () => jest.fn());

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

describe('IndexSetFieldTypesList', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        displayedAttributes: ['name',
          'description',
          'type',
          'custom_field_mappings'],
      },
      isInitialLoading: false,
    });

    asMock(useFieldTypesForMappings).mockReturnValue({
      data: {
        fieldTypes: {
          string: 'String type',
          int: 'Number(int)',
          bool: 'Boolean',
          ip: 'IP',
        },
      },
      isLoading: false,
    });
  });

  it('Shows list of field type profiles with correct data', async () => {
    asMock(useProfiles).mockReturnValue({
      isLoading: false,
      refetch: () => {},
      data: getData([profile1, profile2]),
    });

    renderIndexSetFieldTypeProfilesList();
    const tableRow1 = await screen.findByTestId('table-row-111');

    await within(tableRow1).findByText('Profile 1');
    await within(tableRow1).findByText('Description 1');
    await within(tableRow1).findByText('2');
    await within(tableRow1).findByText('Edit');

    const tableRow2 = await screen.findByTestId('table-row-222');

    await within(tableRow2).findByText('Profile 2');
    await within(tableRow2).findByText('Description 2');
    await within(tableRow2).findByText('3');
    await within(tableRow2).findByText('Edit');
  });

  it('Shows list of Custom Field Mappings for profile', async () => {
    asMock(useProfiles).mockReturnValue({
      isLoading: false,
      refetch: () => {},
      data: getData([profile1, profile2]),
    });

    renderIndexSetFieldTypeProfilesList();

    const tableRow2 = await screen.findByTestId('table-row-222');

    const customFieldTypeMappingAmount = await within(tableRow2).findByText('3');

    fireEvent.click(customFieldTypeMappingAmount);

    expect(tableRow2.textContent).toContain('Custom Field Mappings');
    expect(tableRow2.textContent).toContain('user_name:String type');
    expect(tableRow2.textContent).toContain('logged_in:Boolean');
    expect(tableRow2.textContent).toContain('sum:Number(int)');
  });
});
