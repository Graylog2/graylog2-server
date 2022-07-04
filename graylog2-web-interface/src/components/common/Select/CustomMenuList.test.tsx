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

import CustomMenuList from './CustomMenuList';

const getChildrenList: Function = (n: number): React.ReactElement[] => {
  const list = Array(n).fill(null);

  return list.map(() => <div key={Math.random()} />);
};

describe('CustomMenuList', () => {
  it('Check if List component rendered for number of items more than 1000', () => {
    render(
      <CustomMenuList getStyles={() => {}}
                      cx={() => ({})}>{getChildrenList(1001)}
      </CustomMenuList>,
    );

    const list = screen.getAllByTestId('react-virtualized-list-item');

    expect(list.length).toBeGreaterThan(0);
  });

  it('Check if List component rendered for number of items less than 1000', () => {
    render(
      <CustomMenuList getStyles={() => {}}
                      cx={() => ({})}>{getChildrenList(999)}
      </CustomMenuList>,
    );

    const list = screen.getByTestId('react-select-list');

    expect(list).toBeInTheDocument();
  });
});
