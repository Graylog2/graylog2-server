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
import React from 'react';
import { renderHook, act } from 'wrappedTestingLibrary';
import Immutable from 'immutable';

import useMessageFavoriteFieldsForEditing from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import type { FormattedField } from 'views/components/messagelist/MessageFields/types';
import FieldType from 'views/logic/fieldtypes/FieldType';

const DEFAULT_FIELDS = ['source', 'destination_ip', 'usernames'];

jest.mock('views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry', () => jest.fn);

type ProviderProps = {
  initialFavorites?: Array<string>;
  saveMock?: jest.Mock;
  toggleFieldMock?: jest.Mock;
};

const createWrapper =
  ({ initialFavorites = ['one', 'two'], toggleFieldMock = jest.fn(), saveMock = jest.fn() }: ProviderProps = {}) =>
  ({ children }: { children: React.ReactNode }) => (
    <MessageFavoriteFieldsContext.Provider
      value={{
        favoriteFields: initialFavorites,
        saveFavoriteField: saveMock,
        toggleField: toggleFieldMock,
        messageFields: Immutable.List([]),
        message: undefined,
        editableStreams: [],
        setFieldsIsPending: false,
      }}>
      {children}
    </MessageFavoriteFieldsContext.Provider>
  );

describe('useMessageFavoriteFieldsForEditing', () => {
  it('initializes with favoriteFields from context', () => {
    const wrapper = createWrapper({ initialFavorites: ['fav1', 'fav2'] });
    const { result } = renderHook(() => useMessageFavoriteFieldsForEditing(), { wrapper });

    expect(result.current.editingFavoriteFields).toEqual(['fav1', 'fav2']);
  });

  it('resetFavoriteFields resets to DEFAULT_FIELDS and calls saveFavoriteField', () => {
    const saveMock = jest.fn();
    const wrapper = createWrapper({ initialFavorites: ['a', 'b'], saveMock });
    const { result } = renderHook(() => useMessageFavoriteFieldsForEditing(), { wrapper });

    act(() => {
      result.current.resetFavoriteFields();
    });

    expect(saveMock).toHaveBeenCalledWith(DEFAULT_FIELDS);
    expect(result.current.editingFavoriteFields).toEqual(DEFAULT_FIELDS);
  });

  it('saveEditedFavoriteFields calls saveFavoriteField with current favorites', () => {
    const saveMock = jest.fn();
    const wrapper = createWrapper({ initialFavorites: ['fav1'], saveMock });
    const { result } = renderHook(() => useMessageFavoriteFieldsForEditing(), { wrapper });

    act(() => {
      result.current.onFavoriteToggle('newField'); // add
    });

    act(() => {
      result.current.saveEditedFavoriteFields();
    });

    expect(saveMock).toHaveBeenCalledWith(expect.arrayContaining(['fav1', 'newField']));
  });

  it('reorderFavoriteFields updates favorites in the provided order', () => {
    const wrapper = createWrapper({ initialFavorites: ['fav1', 'fav2', 'fav3'] });
    const { result } = renderHook(() => useMessageFavoriteFieldsForEditing(), { wrapper });

    const items: Array<FormattedField> = [
      {
        field: 'fav3',
        type: FieldType.create('STRING'),
        id: 'fav3',
        value: 'v3',
      },
      { field: 'fav1', type: FieldType.create('STRING'), id: 'fav1', value: 'v1' },
      { field: 'fav2', type: FieldType.create('STRING'), id: 'fav2', value: 'v2' },
    ];

    act(() => {
      result.current.reorderFavoriteFields(items);
    });

    expect(result.current.editingFavoriteFields).toEqual(['fav3', 'fav1', 'fav2']);
  });

  it('onFavoriteToggle removes an existing favorite and adds a new one (avoids duplicates)', () => {
    const wrapper = createWrapper({ initialFavorites: ['fav1', 'fav2'] });
    const { result } = renderHook(() => useMessageFavoriteFieldsForEditing(), { wrapper });

    act(() => {
      result.current.onFavoriteToggle('fav1');
    });
    expect(result.current.editingFavoriteFields).toEqual(['fav2']);

    act(() => {
      result.current.onFavoriteToggle('fav3');
    });
    expect(result.current.editingFavoriteFields).toEqual(expect.arrayContaining(['fav2', 'fav3']));

    act(() => {
      result.current.onFavoriteToggle('fav3');
    });

    expect(result.current.editingFavoriteFields).not.toContain('fav3');
  });
});
