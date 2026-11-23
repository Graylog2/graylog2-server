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
import userEvent from '@testing-library/user-event';

import Routes from 'routing/Routes';
import SelectEntitiesContext from 'components/common/EntityDataTable/contexts/SelectEntitiesContext';
import BulkActions from 'components/indices/IndexSetFieldTypes/BulkActions';
import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';

const mockPushWithState = jest.fn();

jest.mock('routing/useHistory', () => {
  const actual = jest.requireActual('routing/useHistory');

  return {
    __esModule: true,
    default: () => ({ ...actual.default(), pushWithState: mockPushWithState }),
  };
});

const selectedEntitiesData: Record<string, IndexSetFieldType> = {
  'field-1': { id: 'field-1', fieldName: 'http_method', origin: 'INDEX', isReserved: false, type: 'string' },
  'field-2': { id: 'field-2', fieldName: 'took_ms', origin: 'OVERRIDDEN_INDEX', isReserved: false, type: 'long' },
};

const selectEntitiesContextValue = {
  selectedEntities: Object.keys(selectedEntitiesData),
  setSelectedEntities: jest.fn(),
  selectEntity: jest.fn(),
  deselectEntity: jest.fn(),
  toggleEntitySelect: jest.fn(),
  isAllRowsSelected: true,
  isSomeRowsSelected: false,
};

describe('IndexSetFieldTypes BulkActions', () => {
  beforeEach(() => {
    mockPushWithState.mockClear();
  });

  it('opens the create profile form with only the mappings of the selected fields', async () => {
    render(
      <SelectEntitiesContext.Provider value={selectEntitiesContextValue}>
        <BulkActions indexSetId="111" selectedEntitiesData={selectedEntitiesData} />
      </SelectEntitiesContext.Provider>,
    );

    await userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /create new profile/i }));

    expect(mockPushWithState).toHaveBeenCalledWith(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE, {
      customFieldMappings: [
        { field: 'http_method', type: 'string' },
        { field: 'took_ms', type: 'long' },
      ],
    });
  });
});
