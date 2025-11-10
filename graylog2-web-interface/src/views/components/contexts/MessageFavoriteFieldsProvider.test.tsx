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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import Immutable from 'immutable';

import MessageFavoriteFieldsProvider from 'views/components/contexts/MessageFavoriteFieldsProvider';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import { asMock } from 'helpers/mocking';
import useMessageFavoriteFields from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFields';
import { Button } from 'components/bootstrap';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';

// Mocks for useMessageFavoriteFields and DEFAULT_FIELDS
const mockSaveFields = jest.fn();
const DEFAULT_FIELDS = ['default1', 'default2'];

jest.mock('views/components/messagelist/MessageFields/hooks/useMessageFavoriteFields', () => ({
  __esModule: true,
  default: jest.fn(),
  DEFAULT_FIELDS,
}));

const Consumer = () => {
  const contextValue = React.useContext(MessageFavoriteFieldsContext);

  return (
    <div>
      <div data-testid="favorites">{JSON.stringify(contextValue.favoriteFields)}</div>
      <Button onClick={() => contextValue.saveFavoriteField(['field1', 'field2', 'new'])}>save-action</Button>
    </div>
  );
};

const renderComponent = () =>
  render(
    <MessageFavoriteFieldsProvider
      isFeatureEnabled
      message={{ id: 'id', index: 'index', fields: { streams: ['stream1'] } }}
      messageFields={Immutable.List([
        FieldTypeMapping.create('fav1', FieldTypes.STRING()),
        FieldTypeMapping.create('rest1', FieldTypes.STRING()),
      ])}>
      <Consumer />
    </MessageFavoriteFieldsProvider>,
  );

describe('MessageFavoriteFieldsProvider', () => {
  beforeEach(() => {
    asMock(useMessageFavoriteFields).mockReturnValue({
      isLoading: false,
      saveFields: mockSaveFields,
      favoriteFields: ['field1', 'field2'],
    });
  });
  it('initializes favorites from hook and exposes them via context', () => {
    renderComponent();

    expect(screen.getByTestId('favorites')).toHaveTextContent(JSON.stringify(['field1', 'field2']));
  });

  it('saveFavoriteField calls saveFields', async () => {
    renderComponent();

    userEvent.click(await screen.findByRole('button', { name: /save/i }));

    expect(mockSaveFields).toHaveBeenCalledWith(['field1', 'field2', 'new']);
  });
});
