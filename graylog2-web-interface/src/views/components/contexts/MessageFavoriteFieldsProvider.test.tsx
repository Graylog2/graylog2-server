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

import React, { useContext } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import Immutable from 'immutable';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import MessageFavoriteFieldsProvider from 'views/components/contexts/MessageFavoriteFieldsProvider';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import useMessageFavoriteFieldsMutation from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsMutation';
import { Button } from 'components/bootstrap';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import { StreamsActions } from 'views/stores/StreamsStore';
import mockAction from 'helpers/mocking/MockAction';
import type { Stream } from 'logic/streams/types';

const mockSaveFields = jest.fn();
const mockToggleField = jest.fn();

jest.mock('views/stores/StreamsStore');

jest.mock('views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsMutation', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('views/stores/StreamsStore', () => ({
  StreamsActions: { refresh: jest.fn() },
  StreamsStore: MockStore([
    'getInitialState',
    () => ({
      streams: [
        { favorite_fields: ['field1'], id: 'stream1' },
        { favorite_fields: ['field2'], id: 'stream2' },
      ] as Array<Stream>,
    }),
  ]),
}));

const Consumer = () => {
  const contextValue = useContext(MessageFavoriteFieldsContext);

  return (
    <div>
      <div data-testid="favorites">{JSON.stringify(contextValue.favoriteFields)}</div>
      <Button onClick={() => contextValue.saveFavoriteField(['field1', 'field2', 'new'])}>save-action</Button>
      <Button onClick={() => contextValue.toggleField('field3')}>toggle-action</Button>
    </div>
  );
};

const renderComponent = () =>
  render(
    <MessageFavoriteFieldsProvider
      isFeatureEnabled
      message={{ id: 'id', index: 'index', fields: { streams: ['stream1', 'stream2'] } }}
      messageFields={Immutable.List([
        FieldTypeMapping.create('fav1', FieldTypes.STRING()),
        FieldTypeMapping.create('rest1', FieldTypes.STRING()),
      ])}>
      <Consumer />
    </MessageFavoriteFieldsProvider>,
  );

describe('MessageFavoriteFieldsProvider', () => {
  beforeEach(() => {
    StreamsActions.refresh = mockAction();
    asMock(useMessageFavoriteFieldsMutation).mockReturnValue({
      saveFavoriteField: mockSaveFields,
      toggleField: mockToggleField,
      setFieldsIsPending: false,
    });
  });
  it('initializes favorites from streams and exposes them via context', () => {
    renderComponent();

    expect(screen.getByTestId('favorites')).toHaveTextContent(JSON.stringify(['field1', 'field2']));
  });

  it('saveFavoriteField calls saveFavoriteField from hook', async () => {
    renderComponent();

    userEvent.click(await screen.findByRole('button', { name: /save-action/i }));

    expect(mockSaveFields).toHaveBeenCalledWith(['field1', 'field2', 'new']);
  });

  it('toggleField calls toggleField from hook', async () => {
    renderComponent();

    userEvent.click(await screen.findByRole('button', { name: /toggle-action/i }));

    expect(mockToggleField).toHaveBeenCalledWith('field3');
  });
});
