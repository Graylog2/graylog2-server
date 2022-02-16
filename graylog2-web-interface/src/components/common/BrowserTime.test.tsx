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

import BrowserTime from './BrowserTime';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

jest.mock('util/DateTime', () => {
  const DateTime = jest.requireActual('util/DateTime');
  DateTime.getBrowserTimezone('America/Chicago');

  return DateTime;
});

describe('BrowserTime', () => {
  it('should display browser time', () => {
    render(
      <BrowserTime dateTime="2020-01-01T10:00:00.000Z" />,
    );

    expect(screen.getByText('2020-01-01 04:00:00')).toBeInTheDocument();
  });

  it('should display browser time in a specific format', () => {
    render(
      <BrowserTime dateTime="2020-01-01T10:00:00.000Z" format="withTz" />,
    );

    expect(screen.getByText('2020-01-01 04:00:00 -06:00')).toBeInTheDocument();
  });

  it('should display current browser time, when date time is not defined', () => {
    render(<BrowserTime />);

    expect(screen.getByText('2019-12-31 18:00:00')).toBeInTheDocument();
  });

  it('should display current browser time, when date time is undefined', () => {
    render(<BrowserTime dateTime={undefined} />);

    expect(screen.getByText('2019-12-31 18:00:00')).toBeInTheDocument();
  });

  it('should display current browser time, when date time is null', () => {
    render(<BrowserTime dateTime={null} />);

    expect(screen.getByText('2019-12-31 18:00:00')).toBeInTheDocument();
  });
});
