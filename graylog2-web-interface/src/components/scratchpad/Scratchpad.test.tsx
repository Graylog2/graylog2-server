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
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';

import Scratchpad from './Scratchpad';

jest.mock('hooks/useHotkey', () => jest.fn());

const setScratchpadVisibility = jest.fn();
document.execCommand = jest.fn();

const SUT = () => (
  <ScratchpadContext.Provider
    value={{
      isScratchpadVisible: true,
      localStorageItem: 'gl-scratchpad-jest',
      setScratchpadVisibility,
      toggleScratchpadVisibility: jest.fn(),
    }}>
    <Scratchpad />
  </ScratchpadContext.Provider>
);

describe('<Scratchpad />', () => {
  it('properly renders', async () => {
    render(<SUT />);

    await screen.findByRole('dialog');
    await screen.findByRole('heading', { name: /scratchpad/i });
    await screen.findByRole('button', { name: /copy/i });
    await screen.findByRole('button', { name: /clear/i });
    await screen.findByRole('textbox');
  });

  it('renders & dismisses alert', async () => {
    const { rerender } = render(<SUT />);

    const alert = screen.getByRole('alert');
    const btnGotIt = screen.getByRole('button', {
      name: /got it!/i,
    });

    expect(alert).toBeInTheDocument();

    await userEvent.click(btnGotIt);

    rerender(<SUT />);

    expect(alert).not.toBeInTheDocument();
  });

  it('calls setScratchpadVisibility on close', async () => {
    render(<SUT />);

    const btnClose = screen.getByRole('button', { name: /close/i });

    await userEvent.click(btnClose);

    expect(setScratchpadVisibility).toHaveBeenCalledWith(false);
  });

  it('changes textarea & shows auto saved message', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox');
    textarea.focus();

    await userEvent.type(textarea, 'foo');

    await screen.findByText(/auto saved\./i);
  });

  it('shows copied status', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox');
    textarea.focus();
    await userEvent.type(textarea, 'foo');

    const btnCopy = screen.getByRole('button', { name: /copy/i });

    await userEvent.click(btnCopy);

    await screen.findByText(/copied!/i);

    expect(document.execCommand).toHaveBeenCalledWith('copy');
  });

  it('confirms before clearing data', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement;
    textarea.focus();
    await userEvent.type(textarea, 'foo');

    const btnClear = screen.getByRole('button', { name: /clear/i });

    await userEvent.click(btnClear);

    const confirmBtn = await screen.findByRole('button', { name: /confirm/i });

    await userEvent.click(confirmBtn);

    await screen.findByText(/cleared\./i);

    await waitFor(() => expect(textarea.value).toBe(''));
  });
});
