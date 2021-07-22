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
import Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { createSimpleMessageEventType, createSimpleExternalAction } from 'fixtures/messageEventTypes';

import MessageEventTypesProvider from './MessageEventTypesProvider';
import MessageEventTypesContext from './MessageEventTypesContext';

import asMock from '../../../../test/helpers/mocking/AsMock';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

const simpleEventType = createSimpleMessageEventType();
const simpleExternalAction = createSimpleExternalAction();

const mockSecurityContent = {
  eventTypes: [simpleEventType],
  externalActions: [simpleExternalAction],
};

const simpleContextValue = {
  eventTypes: Immutable.Map({ [simpleEventType.gl2EventTypeCode]: simpleEventType }),
  eventActions: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction }),
  fieldValueActions: Immutable.Map({
    field1: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction }),
    field2: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction }),
  }),
};

describe('MessageEventTypesProvider', () => {
  const renderSUT = (consume) => render(
    <MessageEventTypesProvider>
      <MessageEventTypesContext.Consumer>
        {consume}
      </MessageEventTypesContext.Consumer>
    </MessageEventTypesProvider>,
  );

  it('renders children when there is no security content in plugin store', () => {
    render(<MessageEventTypesProvider><>The content</></MessageEventTypesProvider>);

    expect(screen.getByText('The content')).toBeInTheDocument();
  });

  it('provides event types, external actions and fieldValueActions', () => {
    asMock(PluginStore.exports).mockImplementation((type) => ({
      securityContent: [mockSecurityContent],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    expect(contextValue).toStrictEqual(simpleContextValue);
  });

  it('provides correct data, when plugin store contains multiple entries for security content', () => {
    const simpleEventType2 = createSimpleMessageEventType(2);
    const simpleExternalAction2 = createSimpleExternalAction(2);

    const mockSecurityContent2 = {
      eventTypes: [simpleEventType2],
      externalActions: [simpleExternalAction2],
    };

    const newSimpleContextValue = {
      eventTypes: Immutable.Map({ [simpleEventType.gl2EventTypeCode]: simpleEventType, [simpleEventType2.gl2EventTypeCode]: simpleEventType2 }),
      eventActions: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction, [simpleExternalAction2.id]: simpleExternalAction2 }),
      fieldValueActions: Immutable.Map({
        field1: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction, [simpleExternalAction2.id]: simpleExternalAction2 }),
        field2: Immutable.Map({ [simpleExternalAction.id]: simpleExternalAction, [simpleExternalAction2.id]: simpleExternalAction2 }),
      }),
    };

    asMock(PluginStore.exports).mockImplementation((type) => ({
      securityContent: [mockSecurityContent, mockSecurityContent2],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    expect(contextValue).toStrictEqual(newSimpleContextValue);
  });
});
