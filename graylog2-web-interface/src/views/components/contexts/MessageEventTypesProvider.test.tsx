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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { createSimpleMessageEventType } from 'fixtures/messageEventTypes';

import MessageEventTypesProvider from './MessageEventTypesProvider';
import MessageEventTypesContext from './MessageEventTypesContext';

import asMock from '../../../../test/helpers/mocking/AsMock';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

const simpleEventType = createSimpleMessageEventType();

const simpleContextValue = {
  eventTypes: { [simpleEventType.gl2EventTypeCode]: simpleEventType },
};

describe('MessageEventTypesProvider', () => {
  const renderSUT = (consume) => render(
    <MessageEventTypesProvider>
      <MessageEventTypesContext.Consumer>
        {consume}
      </MessageEventTypesContext.Consumer>
    </MessageEventTypesProvider>,
  );

  it('renders children when there are no message event types in plugin store', () => {
    render(<MessageEventTypesProvider><>The content</></MessageEventTypesProvider>);

    expect(screen.getByText('The content')).toBeInTheDocument();
  });

  it('provides message event types from plugin store', () => {
    asMock(PluginStore.exports).mockImplementation((type) => ({
      messageEventTypes: [{ [simpleEventType.gl2EventTypeCode]: simpleEventType }],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    expect(contextValue).toStrictEqual(simpleContextValue);
  });

  it('provides correct data, when plugin store contains multiple entries for message event types', () => {
    const simpleEventType2 = createSimpleMessageEventType(2);

    asMock(PluginStore.exports).mockImplementation((type) => ({
      messageEventTypes: [
        { [simpleEventType.gl2EventTypeCode]: simpleEventType },
        { [simpleEventType2.gl2EventTypeCode]: simpleEventType2 },
      ],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    const expectedContextValue = {
      eventTypes: { [simpleEventType.gl2EventTypeCode]: simpleEventType, [simpleEventType2.gl2EventTypeCode]: simpleEventType2 },
    };

    expect(contextValue).toStrictEqual(expectedContextValue);
  });
});
