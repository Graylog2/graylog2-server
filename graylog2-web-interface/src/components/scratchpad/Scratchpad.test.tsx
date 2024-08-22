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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';

import Scratchpad from './Scratchpad';

jest.mock('hooks/useHotkey', () => jest.fn());

const setScratchpadVisibility = jest.fn();
document.execCommand = jest.fn();

const SUT = () => (
  <ScratchpadContext.Provider value={{
    isScratchpadVisible: true,
    localStorageItem: 'gl-scratchpad-jest',
    setScratchpadVisibility,
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

  it('renders & dismisses alert', () => {
    const { rerender } = render(<SUT />);

    const alert = screen.getByRole('alert');
    const btnGotIt = screen.getByRole('button', {
      name: /got it!/i,
    });

    expect(alert).toBeInTheDocument();

    fireEvent.click(btnGotIt);

    rerender(<SUT />);

    expect(alert).not.toBeInTheDocument();
  });

  it('calls setScratchpadVisibility on close', () => {
    render(<SUT />);

    const btnClose = screen.getByRole('button', { name: /close/i });

    fireEvent.click(btnClose);

    expect(setScratchpadVisibility).toHaveBeenCalledWith(false);
  });

  it('changes textarea & shows auto saved message', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox');
    textarea.focus();

    fireEvent.change(textarea, { target: { value: 'foo' } });

    await screen.findByText(/auto saved\./i);
  });

  it('shows copied status', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox');
    textarea.focus();
    fireEvent.change(textarea, { target: { value: 'foo' } });

    const btnCopy = screen.getByRole('button', { name: /copy/i });

    fireEvent.click(btnCopy);

    await screen.findByText(/copied!/i);

    expect(document.execCommand).toHaveBeenCalledWith('copy');
  });

  it('confirms before clearing data', async () => {
    render(<SUT />);

    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement;
    textarea.focus();
    fireEvent.change(textarea, { target: { value: 'foo' } });

    const btnClear = screen.getByRole('button', { name: /clear/i });

    fireEvent.click(btnClear);

    await screen.findByRole('alertdialog', { hidden: true });

    const confirmBtn = screen.getByRole('button', { name: /confirm/i, hidden: true });

    fireEvent.click(confirmBtn);

    await screen.findByText(/cleared\./i);

    await waitFor(() => {
      expect(textarea.value).toBe('');
    });
  });
});
