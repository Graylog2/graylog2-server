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
import asMock from 'helpers/mocking/AsMock';

import usePluginEntities from 'views/logic/usePluginEntities';
import FieldType from 'views/logic/fieldtypes/FieldType';

import MessagePreview from './MessagePreview';

const simpleEventType = createSimpleMessageEventType(1, { summaryTemplate: '{field1} - {field2}', gl2EventTypeCode: 'event-type-code' });
const mockMessageEventTypes = [simpleEventType];

jest.mock('views/logic/usePluginEntities', () => jest.fn(() => mockMessageEventTypes));

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

  type Props = Partial<React.ComponentProps<typeof MessageSummaryRow>>

  const SUT = ({ message = simpleMessage, ...rest }: Props) => (
    <table>
      <tbody>
        <MessagePreview message={message}
                        onRowClick={() => {}}
                        colSpanFixup={1}
                        showSummaryRow
                        showMessageRow
                        preferSummaryRow={false}
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
    render(<SUT showSummaryRow />);

    expect(screen.getByText('Value for field 1 - Value for field 2')).toBeInTheDocument();
  });

  it('does not display message summary when `showSummaryRow` is false', () => {
    render(<SUT showSummaryRow={false} />);

    expect(screen.queryByText('Value for field 1 - Value for field 2')).not.toBeInTheDocument();
  });

  // it('displays message summary instead of message field when `preferSummaryRow` is true', () => {
  //   render(<SUT showSummaryRow showMessageRow preferSummaryRow />);
  //
  //   expect(screen.getByText('Value for field 1 - Value for field 2')).toBeInTheDocument();
  //   expect(screen.queryByText('Something happened!')).not.toBeInTheDocument();
  // });

  it('displays message summary color based on event category', () => {
    const messageEventType = { ...simpleEventType, category: 'success' } as const;

    asMock(usePluginEntities).mockReturnValue([messageEventType]);

    render(<SUT />);

    expect(screen.getByText('Value for field 1 - Value for field 2')).toHaveStyle('color: #00752c');
  });
});
