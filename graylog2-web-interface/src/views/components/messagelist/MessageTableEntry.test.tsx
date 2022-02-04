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
import MockAction from 'helpers/mocking/MockAction';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';

import MessageTableEntry from './MessageTableEntry';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
  ConfigurationsActions: {
    listSearchesClusterConfig: MockAction(),
  },
}));

jest.mock('hooks/useUserDateTime');

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
                           config={MessagesWidgetConfig.builder().build()}
                           selectedFields={Immutable.OrderedSet(['message', 'notexisting'])}
                           expanded={false} />
      </table>,
    );

    expect(screen.getByText('Something happened!')).toBeInTheDocument();
  });
});
