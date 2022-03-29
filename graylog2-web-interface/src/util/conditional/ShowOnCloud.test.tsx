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

import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

import ShowOnCloud from './ShowOnCloud';

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

describe('ShowOnCloud', () => {
  it('display children on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);

    render(<ShowOnCloud>The Content</ShowOnCloud>);

    expect(screen.getByText('The Content')).toBeInTheDocument();
  });

  it('do not display children when not on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(false);

    render(<ShowOnCloud>The Content</ShowOnCloud>);

    expect(screen.queryByText('The Content')).not.toBeInTheDocument();
  });
});
