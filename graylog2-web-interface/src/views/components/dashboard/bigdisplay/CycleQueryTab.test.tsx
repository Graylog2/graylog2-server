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
// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import { ViewActions } from 'views/stores/ViewStore';

import CycleQueryTab from './CycleQueryTab';

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    selectQuery: jest.fn(),
  },
}));

jest.useFakeTimers();

describe('CycleQueryTab', () => {
  const search = Search.create().toBuilder().queries([
    Query.builder().id('foo').build(),
    Query.builder().id('bar').build(),
    Query.builder().id('baz').build(),
  ]).build();
  const view = View.create().toBuilder().search(search).build();

  beforeEach(() => { jest.clearAllMocks(); });

  it('does not return markup', () => {
    const { container } = render(<CycleQueryTab view={view} activeQuery="bar" interval={1} tabs={[1, 2]} />);

    expect(container.firstChild).toEqual(null);
  });

  it('should not switch to anything before interval', () => {
    render(<CycleQueryTab view={view} activeQuery="bar" interval={1} tabs={[1, 2]} />);

    jest.advanceTimersByTime(900);

    expect(ViewActions.selectQuery).not.toHaveBeenCalled();
  });

  it('should switch to next tab after interval', () => {
    render(<CycleQueryTab view={view} activeQuery="bar" interval={1} tabs={[1, 2]} />);

    jest.advanceTimersByTime(1000);

    expect(ViewActions.selectQuery).toHaveBeenCalledWith('baz');
  });

  it('should switch to first tab if current one is the last', () => {
    render(<CycleQueryTab view={view} activeQuery="baz" interval={1} tabs={[0, 1, 2]} />);

    jest.advanceTimersByTime(1000);

    expect(ViewActions.selectQuery).toHaveBeenCalledWith('foo');
  });

  it('should switch to next tab skipping gaps after interval', () => {
    render(<CycleQueryTab view={view} activeQuery="foo" interval={1} tabs={[0, 2]} />);

    jest.advanceTimersByTime(1000);

    expect(ViewActions.selectQuery).toHaveBeenCalledWith('baz');
  });

  it('should switch to next tab defaulting to all tabs if `tabs` prop` is left out', () => {
    render(<CycleQueryTab view={view} activeQuery="foo" tabs={[1]} interval={1} />);

    jest.advanceTimersByTime(1000);

    expect(ViewActions.selectQuery).toHaveBeenCalledWith('bar');
  });

  it('triggers tab change after the correct interval has passed', async () => {
    render(<CycleQueryTab view={view} activeQuery="foo" interval={42} />);

    jest.advanceTimersByTime(42000);

    expect(ViewActions.selectQuery).toHaveBeenCalledTimes(1);
  });

  it('does not trigger after unmounting', () => {
    const { unmount } = render(<CycleQueryTab view={view} activeQuery="foo" interval={42} />);

    unmount();

    jest.advanceTimersByTime(42000);

    expect(ViewActions.selectQuery).not.toHaveBeenCalled();
  });
});
