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
import * as Immutable from 'immutable';
import MockStore from 'helpers/mocking/StoreMock';
import { createSimpleMessageEventType } from 'fixtures/messageEventTypes';
import type { ExternalValueAction } from 'views/types';

import MessageEventTypesContext from 'views/components/contexts/MessageEventTypesContext';

import MessageTableEntry from './MessageTableEntry';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
}));

describe('MessageTableEntry', () => {
  it('renders message for unknown selected fields', () => {
    const message = {
      id: 'deadbeef',
      index: 'test_0',
      fields: {
        message: 'Something happened!',
      },
    };

    render(
      <table>
        <MessageTableEntry expandAllRenderAsync
                           toggleDetail={() => {}}
                           fields={Immutable.List()}
                           message={message}
                           selectedFields={Immutable.OrderedSet(['message', 'notexisting'])}
                           expanded={false} />
      </table>,
    );

    expect(screen.getByText('Something happened!')).toBeInTheDocument();
  });

  it('displays message summary', () => {
    const simpleEventType = createSimpleMessageEventType(1, { summary: '{field1} - {field2}', gl2EventTypeCode: 'event-type-code' });

    const messageEvents = {
      eventTypes: Immutable.Map({ [simpleEventType.gl2EventTypeCode]: simpleEventType }),
      eventActions: Immutable.Map<ExternalValueAction['id'], ExternalValueAction>(),
      fieldValueActions: Immutable.Map<string, Array<ExternalValueAction>>(),
    };

    const message = {
      id: 'deadbeef',
      index: 'test_0',
      fields: {
        gl2_event_type_code: 'event-type-code',
        message: 'Something happened!',
        field1: 'Value for field 1',
        field2: 'Value for field 2',
      },
    };

    render(
      <MessageEventTypesContext.Provider value={messageEvents}>
        <table>
          <MessageTableEntry expandAllRenderAsync
                             toggleDetail={() => {}}
                             fields={Immutable.List()}
                             message={message}
                             selectedFields={Immutable.OrderedSet(['message'])}
                             expanded={false} />
        </table>,
      </MessageEventTypesContext.Provider>,
    );

    expect(screen.getByText('Value for field 1 - Value for field 2')).toBeInTheDocument();
  });
});
