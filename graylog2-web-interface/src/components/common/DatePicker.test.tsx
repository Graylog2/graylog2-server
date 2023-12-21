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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import { alice } from 'fixtures/users';

import DatePicker from './DatePicker';

const mockCurrentUser = alice.toBuilder()
  .timezone('Europe/Berlin')
  .build();

jest.mock('hooks/useCurrentUser', () => () => mockCurrentUser);

describe('DatePicker', () => {
  it('should highlight selected day', async () => {
    render(<DatePicker date="2023-10-19" onChange={() => {}} />);

    expect(await screen.findByText('19')).toHaveAttribute('aria-selected', 'true');
  });
});
