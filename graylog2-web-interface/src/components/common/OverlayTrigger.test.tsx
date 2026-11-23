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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import OverlayTrigger from './OverlayTrigger';

type Props = Pick<React.ComponentProps<typeof OverlayTrigger>, 'trigger'>;

const SUT = ({ trigger }: Props) => (
  <>
    <OverlayTrigger trigger={trigger} placement="bottom" overlay="Overlay content">
      <button type="button">Show details</button>
    </OverlayTrigger>
    <button type="button">Other button</button>
  </>
);

const triggerButton = () => screen.getByText('Show details');

const expectOverlayToBeClosed = () =>
  waitFor(() => expect(screen.queryByText('Overlay content')).not.toBeInTheDocument());

describe('OverlayTrigger', () => {
  it('toggles the overlay on click', async () => {
    render(<SUT trigger="click" />);

    await userEvent.click(triggerButton());

    await screen.findByText('Overlay content');

    await userEvent.click(triggerButton());

    await expectOverlayToBeClosed();
  });

  it('opens the overlay on hover and closes it when the pointer leaves', async () => {
    render(<SUT trigger="hover" />);

    await userEvent.hover(triggerButton());

    await screen.findByText('Overlay content');

    await userEvent.unhover(triggerButton());

    await expectOverlayToBeClosed();
  });

  it('opens the overlay on focus and closes it when the trigger loses focus', async () => {
    render(<SUT trigger="focus" />);

    await userEvent.tab();

    expect(triggerButton()).toHaveFocus();

    await screen.findByText('Overlay content');

    await userEvent.tab();

    expect(screen.getByRole('button', { name: 'Other button' })).toHaveFocus();

    await expectOverlayToBeClosed();
  });

  it('supports combining hover and focus triggers', async () => {
    render(<SUT trigger={['hover', 'focus']} />);

    await userEvent.hover(triggerButton());

    await screen.findByText('Overlay content');

    await userEvent.unhover(triggerButton());

    await expectOverlayToBeClosed();

    await userEvent.tab();

    await screen.findByText('Overlay content');

    await userEvent.tab();

    await expectOverlayToBeClosed();
  });
});
