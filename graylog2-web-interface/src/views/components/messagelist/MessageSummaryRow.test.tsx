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
import { createSimpleMessageEventType } from 'fixtures/messageEventTypes';

import MessageEventTypesContext, { MessageEventTypesContextType } from 'views/components/contexts/MessageEventTypesContext';

import MessageSummaryRow from './MessageSummaryRow';

describe('MessageSummaryRow', () => {
  const simpleEventType = createSimpleMessageEventType(1, { summaryTemplate: '{field1} - {field2}', gl2EventTypeCode: 'event-type-code' });
  const simpleMessageEventsContextValue = {
    eventTypes: { [simpleEventType.gl2EventTypeCode]: simpleEventType },
  };

  const simpleMessage = {
    id: 'deadbeef',
    index: 'test_0',
    fields: {
      gl2_event_type_code: 'event-type-code',
      message: 'Something happened!',
      field1: 'Value for field 1',
      field2: 'Value for field 2',
    },
  };

  type Props = Partial<React.ComponentProps<typeof MessageSummaryRow>> & {
    messageEventsContextValue?: MessageEventTypesContextType,
  }

  const SUT = ({ messageEventsContextValue = simpleMessageEventsContextValue, message = simpleMessage, ...rest }: Props) => (
    <MessageEventTypesContext.Provider value={messageEventsContextValue}>
      <table>
        <tbody>
          <MessageSummaryRow message={message} {...rest} />
        </tbody>

      </table>,
    </MessageEventTypesContext.Provider>
  );

  it('displays message summary', () => {
    render(<SUT />);

    expect(screen.getByText('Value for field 1 - Value for field 2')).toBeInTheDocument();
  });

  it('displays message summary color based on event category', () => {
    const messageEventsContextValue = {
      eventTypes: { [simpleEventType.gl2EventTypeCode]: { ...simpleEventType, category: 'success' } },
    } as const;

    render(<SUT messageEventsContextValue={messageEventsContextValue} />);

    expect(screen.getByText('Value for field 1 - Value for field 2')).toHaveStyle('color: #00752c');
  });
});
