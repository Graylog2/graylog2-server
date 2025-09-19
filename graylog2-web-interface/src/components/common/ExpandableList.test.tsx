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
  it('should render with a Item', async () => {
    render(
      <ExpandableList>
        <ExpandableListItem header="Wheel of time" value="wheel-of-time">
          <span>Edmonds Field</span>
        </ExpandableListItem>
      </ExpandableList>,
    );

    await screen.findByRole('button', { name: /wheel of time/i });

    expect(await screen.findByText('Edmonds Field')).not.toBeVisible();
  });

  it('should render with a nested ExpandableList', async () => {
    render(
      <ExpandableList>
        <ExpandableListItem header="Wheel of time" value="parent-list-item">
          <ExpandableList>
            <ExpandableListItem header="Edmonds Field" value="child-list-item">
              Child content
            </ExpandableListItem>
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>,
    );

    const parentItem = await screen.findByRole('button', { name: /wheel of time/i });

    userEvent.click(parentItem);

    await screen.findByRole('button', { name: /edmonds field/i });
  });
});
