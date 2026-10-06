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

import MessageShow from 'components/search/MessageShow';

const message = {
  id: 'deadbeef',
  index: 'graylog_0',
  fields: {
    message: 'Some message',
    // Integers outside of the safe range are parsed as BigInt from API responses.
    event: { record_id: BigInt('12345678901234567890') },
  },
};

describe('MessageShow', () => {
  it('renders field values containing integers which exceed the safe integer range', async () => {
    render(<MessageShow message={message} />);

    await screen.findByText('{"record_id":12345678901234567890}');
  });
});
