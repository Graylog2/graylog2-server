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
import { asMock } from 'helpers/mocking';

import usePluginEntities from 'views/logic/usePluginEntities';
import FieldType from 'views/logic/fieldtypes/FieldType';

import MessagePreview from './MessagePreview';

const simpleEventType = createSimpleMessageEventType(1, { summaryTemplate: '{field1} - {field2}', gl2EventTypeCode: 'event-type-code' });
const mockMessageEventTypes = [simpleEventType];

jest.mock('views/logic/usePluginEntities', () => jest.fn((entityKey) => ({
  messageEventTypes: mockMessageEventTypes,
  'views.components.widgets.messageTable.summary': [() => <div>The message summary</div>],
}[entityKey])));

describe('MessagePreview', () => {
  afterEach(() => {
    jest.clearAllTimers();
  });

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

  const SUT = ({ message = simpleMessage, ...rest }: Partial<React.ComponentProps<typeof MessagePreview>>) => (
    <table>
      <tbody>
        <MessagePreview message={message}
                        onRowClick={() => {}}
                        colSpanFixup={1}
                        showSummary={false}
                        showMessageRow
                        messageFieldType={new FieldType('string', [], [])}
                        {...rest} />
      </tbody>
    </table>
  );

  it('displays message field', () => {
    render(<SUT showMessageRow />);

    expect(screen.getByText('Something happened!')).toBeInTheDocument();
  });

  it('does not display message field when `showMessageRow` is false', () => {
    render(<SUT showMessageRow={false} />);

    expect(screen.queryByText('Something happened!')).not.toBeInTheDocument();
  });

  it('displays message summary', () => {
    render(<SUT showSummary showMessageRow />);

    expect(screen.getByText('The message summary')).toBeInTheDocument();
  });

  it('replaces message row with summary, when summary is available', () => {
    render(<SUT showSummary showMessageRow />);

    expect(screen.getByText('The message summary')).toBeInTheDocument();
    expect(screen.queryByText('Something happened!')).not.toBeInTheDocument();
  });

  it('message summary receives message row as fallback', () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      messageEventTypes: mockMessageEventTypes,
      'views.components.widgets.messageTable.summary': [({ renderFallback }) => (
        <div>
          The summary component {renderFallback()}
        </div>
      )],
    }[entityKey]));

    render(<SUT showSummary showMessageRow />);

    expect(screen.queryByText('The message summary')).not.toBeInTheDocument();
    expect(screen.getByText(/The summary component/)).toBeInTheDocument();
    expect(screen.getByText('Something happened!')).toBeInTheDocument();
  });
});
