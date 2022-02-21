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

import Timestamp from './Timestamp';

jest.mock('hooks/useUserDateTime');

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

describe('Timestamp', () => {
  it('should render date time', () => {
    render(<Timestamp dateTime="2020-01-01T10:00:00.000Z" />);

    expect(screen.getByText('2020-01-01 10:00:00')).toBeInTheDocument();
  });

  it('should render date time based on defined time zone', () => {
    render(<Timestamp dateTime="2020-01-01T10:00:00.000Z" tz="Europe/Moscow" />);

    expect(screen.getByText('2020-01-01 13:00:00')).toBeInTheDocument();
  });

  it('should render date time in a defined format and based on defined time zone', () => {
    render(<Timestamp dateTime="2020-01-01T10:00:00.000Z" format="internal" tz="Europe/Moscow" />);

    expect(screen.getByText('2020-01-01T13:00:00.000+03:00')).toBeInTheDocument();
  });

  it('should display current time, when date time is not defined', () => {
    render(<Timestamp />);

    expect(screen.getByText('2020-01-01 00:00:00')).toBeInTheDocument();
  });

  it('should display current time, when date time is undefined', () => {
    render(<Timestamp dateTime={undefined} />);

    expect(screen.getByText('2020-01-01 00:00:00')).toBeInTheDocument();
  });

  it('should display current time, when date time is null', () => {
    render(<Timestamp dateTime={null} />);

    expect(screen.getByText('2020-01-01 00:00:00')).toBeInTheDocument();
  });
});
