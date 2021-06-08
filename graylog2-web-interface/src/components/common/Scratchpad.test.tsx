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
import ClipboardJS from 'clipboard';
import { act } from 'react-dom/test-utils';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';

import Scratchpad from './Scratchpad';

const setScratchpadVisibility = jest.fn();

const renderSUT = () => {
  return (
    <ScratchpadContext.Provider value={{
      isScratchpadVisible: true,
      localStorageItem: 'gl-scratchpad-jest',
      setScratchpadVisibility,
    }}>
      <Scratchpad />
    </ScratchpadContext.Provider>
  );
};

jest.mock('clipboard');

describe('<Scratchpad />', () => {
  it('properly renders', () => {
    render(renderSUT());

    const modal = screen.getByRole('dialog');
    const header = screen.getByRole('heading', { name: /scratchpad/i });
    const btnCopy = screen.getByRole('button', { name: /copy/i });
    const btnClear = screen.getByRole('button', { name: /clear/i });
    const textarea = screen.getByRole('textbox');

    expect(modal).toBeInTheDocument();
    expect(header).toBeInTheDocument();
    expect(btnCopy).toBeInTheDocument();
    expect(btnClear).toBeInTheDocument();
    expect(textarea).toBeInTheDocument();
  });

  it('renders & dismisses alert', () => {
    const { rerender } = render(renderSUT());

    const alert = screen.getByRole('alert');
    const btnGotIt = screen.getByRole('button', {
      name: /got it!/i,
    });

    expect(alert).toBeInTheDocument();

    fireEvent.click(btnGotIt);

    rerender(renderSUT());

    expect(alert).not.toBeInTheDocument();
  });

  it('calls setScratchpadVisibility on close', () => {
    render(renderSUT());

    const btnClose = screen.getByRole('button', { name: /close/i });

    fireEvent.click(btnClose);

    expect(setScratchpadVisibility).toBeCalledWith(false);
  });

  it('changes textarea & shows auto saved message', async () => {
    render(renderSUT());

    const textarea = screen.getByRole('textbox');
    textarea.focus();

    fireEvent.change(textarea, { target: { value: 'foo' } });

    await waitFor(() => expect(screen.getByText(/auto saved\./i)).toBeInTheDocument());
  });

  it('shows copied status', () => {
    render(renderSUT());

    const textarea = screen.getByRole('textbox');
    textarea.focus();
    fireEvent.change(textarea, { target: { value: 'foo' } });

    const btnCopy = screen.getByRole('button', { name: /copy/i });

    const clipboard = new ClipboardJS(btnCopy);

    act(() => {
      fireEvent.click(btnCopy);
    });

    clipboard.on('success', () => {
      expect(screen.getByText(/copied!/i)).toBeInTheDocument();
    });

    expect(clipboard.on).toHaveBeenCalled();
  });

  it('confirms before clearing data', async () => {
    render(renderSUT());

    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement;
    textarea.focus();
    fireEvent.change(textarea, { target: { value: 'foo' } });

    const btnClear = screen.getByRole('button', { name: /clear/i });

    fireEvent.click(btnClear);

    await waitFor(() => {
      expect(screen.getByRole('alertdialog', { hidden: true })).toBeInTheDocument();

      const confirmBtn = screen.getByRole('button', { name: /confirm/i, hidden: true });

      fireEvent.click(confirmBtn);

      expect(screen.getByText(/cleared\./i)).toBeInTheDocument();
      expect(textarea.value).toBe('');
    });
  });
});
