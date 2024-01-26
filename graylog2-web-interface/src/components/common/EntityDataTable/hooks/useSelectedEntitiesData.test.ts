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

import { renderHook } from '@testing-library/react-hooks';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntitiesData from 'components/common/EntityDataTable/hooks/useSelectedEntitiesData';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

jest.mock('./useSelectedEntities');

describe('useSelectedEntitiesData.test hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should add new data when new selected entities appear', async () => {
    const selectedEntities = {
      selectedEntities: ['id-1', 'id-2'],
      selectEntity(id: string) {
        this.selectedEntities.push(id);
      },
      setSelectedEntities: () => {},
      deselectEntity: () => {},
    };
    asMock(useSelectedEntities).mockImplementation(() => selectedEntities);
    const list = {
      'id-1': { id: 'id-1', name: 'name' },
      'id-2': { id: 'id-2', name: 'name' },
      'id-3': { id: 'id-3', name: 'name' },
    };
    const { result, waitFor, rerender } = renderHook((params: Record<string, { id: string, name: string }> = list) => useSelectedEntitiesData(params));
    await waitFor(() => expect(result.current).toEqual([{ id: 'id-1', name: 'name' }, { id: 'id-2', name: 'name' }]));
    selectedEntities.selectEntity('id-5');
    const newList = {
      'id-4': { id: 'id-4', name: 'name' },
      'id-5': { id: 'id-5', name: 'name' },
      'id-6': { id: 'id-6', name: 'name' },
    };
    rerender(newList);
    await waitFor(() => expect(result.current).toEqual([{ id: 'id-1', name: 'name' }, { id: 'id-2', name: 'name' }, { id: 'id-5', name: 'name' }]));
  });
});
