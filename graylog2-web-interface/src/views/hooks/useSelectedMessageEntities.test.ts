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
import { act, renderHook } from 'wrappedTestingLibrary/hooks';
import type { SetStateAction } from 'react';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type { SelectableMessageTableMessage } from 'views/components/widgets/MessageList';

import useSelectedMessageEntities, { createSelectedEntityId } from './useSelectedMessageEntities';

jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');

describe('useSelectedMessageEntities', () => {
  let selectedEntities: Array<string>;

  beforeEach(() => {
    selectedEntities = [];

    asMock(useSelectedEntities).mockImplementation(() => ({
      selectedEntities,
      isAllRowsSelected: false,
      isSomeRowsSelected: selectedEntities.length > 0,
      toggleEntitySelect: jest.fn(),
      setSelectedEntities: (updater: SetStateAction<Array<string>>) => {
        selectedEntities = typeof updater === 'function' ? updater(selectedEntities) : updater;
      },
      selectEntity: jest.fn(),
      deselectEntity: jest.fn(),
    }));
  });

  it('treats messages with the same id in different indices as separate selected entities', () => {
    const data: Array<SelectableMessageTableMessage> = [
      { id: 'deadbeef', index: 'graylog_42', timestamp: '2024-01-01T00:00:00.000Z' },
      { id: 'deadbeef', index: 'graylog_43', timestamp: '2024-01-02T00:00:00.000Z' },
    ];
    const { result, rerender } = renderHook(() => useSelectedMessageEntities());

    act(() => {
      result.current.toggleAllEntitySelect(data);
    });

    rerender();

    expect(result.current.selectedEntities).toEqual([
      { id: 'deadbeef', index: 'graylog_42' },
      { id: 'deadbeef', index: 'graylog_43' },
    ]);
    expect(result.current.isEntitySelected('graylog_42', 'deadbeef')).toBe(true);
    expect(result.current.isEntitySelected('graylog_43', 'deadbeef')).toBe(true);
    expect(selectedEntities).toEqual([
      createSelectedEntityId('graylog_42', 'deadbeef'),
      createSelectedEntityId('graylog_43', 'deadbeef'),
    ]);
  });
});
