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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import Immutable from 'immutable';

import { StoreMock as MockStore } from 'helpers/mocking';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import MessageEditFieldConfigurationAction from 'views/components/messagelist/MessageFields/MessageEditFieldConfigurationAction';
import type { MessageFavoriteFieldsContextState } from 'views/components/contexts/MessageFavoriteFieldsContext';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';

const resetFavoriteField = jest.fn();
const saveFavoriteField = jest.fn();

jest.mock('views/stores/StreamsStore', () => ({
  StreamsActions: { refresh: jest.fn() },
  StreamsStore: MockStore(['getInitialState', () => ({ streams: [{ id: 'streamId', title: 'Stream' }] })]),
}));

const mockedContextValue: MessageFavoriteFieldsContextState = {
  saveFavoriteField,
  resetFavoriteField,
  removeFromFavoriteFields: jest.fn(),
  cancelEdit: jest.fn(),
  addToFavoriteFields: jest.fn(),
  favoriteFields: ['foo'],
  isLoadingFavoriteFields: false,
  setFavorites: jest.fn(),
  messageFields: Immutable.List([
    FieldTypeMapping.create('foo', FieldTypes.STRING()),
    FieldTypeMapping.create('rest1', FieldTypes.STRING()),
  ]),
  message: { id: 'id', index: 'index', fields: { streams: ['streamId'] }, formatted_fields: ['foo', 'rest1'] },
};

const renderComponent = (contextValue: MessageFavoriteFieldsContextState) =>
  render(
    <GraylogThemeProvider userIsLoggedIn>
      <MessageFavoriteFieldsContext.Provider value={contextValue}>
        <MessageEditFieldConfigurationAction />
      </MessageFavoriteFieldsContext.Provider>
    </GraylogThemeProvider>,
  );

describe('MessageEditFieldConfigurationAction', () => {
  it('shows edit modal', async () => {
    renderComponent(mockedContextValue);
    const editButton = await screen.findByRole('button', { name: /edit/i });

    fireEvent.click(editButton);
    await screen.findByRole('heading', { name: /favorite fields configuration/i });
  });
});
