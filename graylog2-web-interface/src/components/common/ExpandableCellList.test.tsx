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
import userEvent from '@testing-library/user-event';

import ExpandableCellList from 'components/common/ExpandableCellList';

type Item = { id: string; name: string };

const items: Array<Item> = [
  { id: 'item-1', name: 'Alpha' },
  { id: 'item-2', name: 'Bravo' },
  { id: 'item-3', name: 'Charlie' },
];

const SUT = (props: Partial<React.ComponentProps<typeof ExpandableCellList<Item>>>) => (
  <ExpandableCellList<Item>
    items={items}
    renderItem={({ name }) => name}
    itemKey={({ id }) => id}
    itemTitle={({ name }) => `Title of ${name}`}
    {...props}
  />
);

describe('ExpandableCellList', () => {
  it('renders nothing when there are no items', async () => {
    render(
      <div data-testid="cell">
        <SUT items={[]} />
      </div>,
    );

    expect(await screen.findByTestId('cell')).toBeEmptyDOMElement();
  });

  it('renders all items without a toggle when they fit', async () => {
    render(<SUT items={items.slice(0, 1)} />);

    await screen.findByText('Alpha');

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('shows the first item and the number of hidden items', async () => {
    render(<SUT />);

    await screen.findByText('Alpha');

    expect(await screen.findByRole('button', { name: /show 2 more/i })).toHaveTextContent('+2');
    expect(screen.queryByText('Bravo')).not.toBeInTheDocument();
    expect(screen.queryByText('Charlie')).not.toBeInTheDocument();
  });

  it('respects a custom collapsed count', async () => {
    render(<SUT collapsedCount={2} />);

    await screen.findByText('Alpha');
    await screen.findByText('Bravo');

    expect(await screen.findByRole('button', { name: /show 1 more/i })).toHaveTextContent('+1');
    expect(screen.queryByText('Charlie')).not.toBeInTheDocument();
  });

  it('expands and collapses the list', async () => {
    render(<SUT />);

    await userEvent.click(await screen.findByRole('button', { name: /show 2 more/i }));

    await screen.findByText('Bravo');
    await screen.findByText('Charlie');

    await userEvent.click(await screen.findByRole('button', { name: /show less/i }));

    await screen.findByRole('button', { name: /show 2 more/i });

    expect(screen.queryByText('Charlie')).not.toBeInTheDocument();
  });

  it('shows the item title on hover', async () => {
    render(<SUT />);

    expect(await screen.findByTitle('Title of Alpha')).toHaveTextContent('Alpha');
  });

  it('does not propagate toggle clicks, e.g. to a clickable row', async () => {
    const onRowClick = jest.fn();

    render(
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
      <div onClick={onRowClick}>
        <SUT />
      </div>,
    );

    await userEvent.click(await screen.findByRole('button', { name: /show 2 more/i }));

    await screen.findByText('Charlie');

    expect(onRowClick).not.toHaveBeenCalled();
  });
});
