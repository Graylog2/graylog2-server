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

import EventDefinitions from './EventDefinitions';

const SUT = (props: Partial<React.ComponentProps<typeof EventDefinitions>>) => (
  <EventDefinitions onDisable={jest.fn()}
                    onDelete={jest.fn()}
                    onCopy={jest.fn()}
                    onEnable={jest.fn()}
                    onPageChange={jest.fn()}
                    query="*"
                    onQueryChange={jest.fn()}
                    pagination={{
                      grandTotal: 0,
                      page: 1,
                      pageSize: 10,
                      total: 0,
                    }}
                    eventDefinitions={[]}
                    {...props} />
);

describe('EventDefinitions', () => {
  it('renders special dialog when no event definitions are present', async () => {
    render(<SUT />);

    await screen.findByText('Looks like there is nothing here, yet!');
  });
});
