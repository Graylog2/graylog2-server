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
import userEvent from '@testing-library/user-event';

import ExpandableList from 'components/common/ExpandableList';
import ExpandableListItem from 'components/common/ExpandableListItem';

describe('<ExpandableList />', () => {
  it('should render with no children', async () => {
    render(<ExpandableList />);

    await screen.findByRole('list');
  });

  it('should render with a Item', async () => {
    render(
      <ExpandableList>
        <ExpandableListItem header="Wheel of time" onChange={() => {}}>
          <span>Edmonds Field</span>
        </ExpandableListItem>
      </ExpandableList>,
    );

    await screen.findByRole('list');
    await screen.findByRole('button', { name: /wheel of time/i });

    expect(screen.queryByText('Edmonds Field')).not.toBeInTheDocument();
  });

  it('should render with a nested ExpandableList', async () => {
    render(
      <ExpandableList>
        <ExpandableListItem expandable expanded header="Wheel of time" onChange={() => {}}>
          <ExpandableList>
            <ExpandableListItem expandable expanded={false} header="Edmonds Field" onChange={() => {}} />
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>,
    );

    await screen.findByRole('button', { name: /wheel of time/i });

    expect(await screen.findAllByRole('list')).toHaveLength(2);

    await screen.findByRole('button', { name: /edmonds field/i });
  });

  it('should expand a expandable list item', async () => {
    render(
      <ExpandableList>
        <ExpandableListItem expandable header="Wheel of time" readOnly>
          <ExpandableList>
            <ExpandableListItem header="Edmonds Field" readOnly />
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>,
    );

    await userEvent.click(await screen.findByRole('button', { name: /expand list item/i }));

    await screen.findByRole('button', { name: /edmonds field/i });
  });

  it('should select a selectable list item', async () => {
    const checkFn = jest.fn();

    render(
      <ExpandableList>
        <ExpandableListItem expanded header="Wheel of time" readOnly>
          <ExpandableList>
            <ExpandableListItem expanded selectable header="Edmonds Field" onChange={checkFn} />
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>,
    );

    await userEvent.click((await screen.findAllByRole('checkbox', { name: /select item/i }))[1]);

    expect(checkFn).toHaveBeenCalledTimes(1);
  });
});
