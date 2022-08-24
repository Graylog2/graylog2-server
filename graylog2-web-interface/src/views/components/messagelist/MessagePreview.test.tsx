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

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';

import MessagePreview from './MessagePreview';

jest.mock('hooks/usePluginEntities', () => jest.fn());

describe('MessagePreview', () => {
  afterEach(() => {
    jest.clearAllTimers();
  });

  const simpleMessage = {
    id: 'deadbeef',
    index: 'test_0',
    fields: {
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
                        showMessageRow
                        config={MessagesWidgetConfig.builder().build()}
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

  it('displays pluggable message row override', () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      'views.components.widgets.messageTable.messageRowOverride': [() => <div>The message row override</div>],
    }[entityKey]));

    render(<SUT showMessageRow />);

    expect(screen.getByText('The message row override')).toBeInTheDocument();
  });

  it('pluggable message row override receives message row renderer as prop', () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      'views.components.widgets.messageTable.messageRowOverride': [({ renderMessageRow }) => (
        <div>
          The message row override {renderMessageRow()}
        </div>
      )],
    }[entityKey]));

    render(<SUT showMessageRow />);

    expect(screen.getByText(/The message row override/)).toBeInTheDocument();
    expect(screen.getByText('Something happened!')).toBeInTheDocument();
  });
});
