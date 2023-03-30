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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';

import AsyncCustomMenuList from './AsyncCustomMenuList';

jest.mock('hooks/useElementDimensions', () => () => ({ width: 1024, height: 300 }));

const getChildrenList: Function = (n: number): React.ReactElement[] => {
  const list = Array(n).fill(null);

  return list.map(() => <div key={Math.random()}>{Math.random()}</div>);
};

describe('CustomMenuList', () => {
  const loadOptions = jest.fn();
  const total = 1000;
  const mockSelectProps = {
    loadOptions,
    total,
  };

  it('Should render AsyncCustomMenuList', () => {
    render(
      <AsyncCustomMenuList getStyles={() => {}}
                           selectProps={mockSelectProps}
                           cx={() => ({})}>
        {getChildrenList(50)}
      </AsyncCustomMenuList>,
    );

    const list = screen.getAllByTestId('react-window-list-item');

    expect(list.length).toBeGreaterThan(0);
  });

  it('Should load more items on scrool', async () => {
    render(
      <AsyncCustomMenuList getStyles={() => {}}
                           selectProps={mockSelectProps}
                           cx={() => ({})}>
        {getChildrenList(5)}
      </AsyncCustomMenuList>,
    );

    const list = screen.getByTestId('infinite-loader-container').firstChild;

    expect(list).toBeInTheDocument();

    fireEvent.scroll(list);

    await waitFor(() => expect(loadOptions).toHaveBeenCalled());
  });
});
