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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { screen, render, within, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import type { Url } from 'stores/configurations/ConfigurationsStore';

import UrlWhiteListForm from './UrlWhiteListForm';

const mockTestRegexValidity = jest.fn(() => Promise.resolve({ is_valid: true }));

jest.mock('stores/tools/ToolsStore', () => ({
  testRegexValidity: () => mockTestRegexValidity(),
}));

const config = {
  entries: [
    {
      id: 'f7033f1f-d50f-4323-96df-294ede41d951',
      value: 'http://localhost:8080/system/1',
      title: 'testam',
      type: 'regex',
    },
    {
      id: '636a2d40-c4c5-40b9-ab3a-48cf7978e9af',
      value: 'http://localhost:8080/system/2',
      title: 'test',
      type: 'regex',
    },
    {
      id: 'f28fd891-5f2d-4128-9a94-e97c1ab07a1f',
      value: 'http://localhost:8080/system/3',
      title: 'test2',
      type: 'literal',
    },
  ],
  disabled: false,
};

describe('UrlWhitelistForm', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should show allow list toggle and url table', () => {
    const onUpdate = jest.fn();

    render(<UrlWhiteListForm urls={config.entries}
                             disabled={config.disabled}
                             onUpdate={onUpdate} />);

    expect(screen.getByRole('checkbox', { name: /disable whitelist/i })).toBeInTheDocument();

    config.entries.forEach(({ title }) => {
      expect(screen.getByDisplayValue(title)).toBeInTheDocument();
    });

    expect(onUpdate).toHaveBeenCalledTimes(1);
  });

  it('should update on input change', async () => {
    const onUpdate = jest.fn();
    const nextValue = 'foobar';

    render(<UrlWhiteListForm urls={config.entries}
                             disabled={config.disabled}
                             onUpdate={onUpdate} />);

    expect(screen.queryByDisplayValue(nextValue)).not.toBeInTheDocument();

    const titleInput = screen.getByDisplayValue(config.entries[0].title);

    userEvent.clear(titleInput);
    userEvent.type(titleInput, nextValue);

    const numberCalls = 2; // First render + debounce
    await waitFor(() => expect(onUpdate).toHaveBeenCalledTimes(numberCalls));

    expect(mockTestRegexValidity).toHaveBeenCalledTimes(numberCalls - 1);

    const expectedEntry = { ...config.entries[0], title: nextValue };

    expect(onUpdate).toHaveBeenLastCalledWith(
      expect.objectContaining({
        entries: expect.arrayContaining([expectedEntry]),
      }),
      true,
    );
  });

  it('should update type changes', async () => {
    const onUpdate = jest.fn();

    render(<UrlWhiteListForm urls={config.entries}
                             disabled={config.disabled}
                             onUpdate={onUpdate} />);

    const row = screen.getByRole('row', { name: /3/i, exact: true });
    const select = within(row).getByText(/exact match/i);

    await selectEvent.openMenu(select);
    await selectEvent.select(select, 'Regex');

    const numberCalls = 2; // First render + debounce
    await waitFor(() => expect(onUpdate).toHaveBeenCalledTimes(numberCalls));

    expect(mockTestRegexValidity).toHaveBeenCalledTimes(numberCalls - 1);

    const expectedEntry = { ...config.entries[0], type: 'regex' };

    expect(onUpdate).toHaveBeenLastCalledWith(
      expect.objectContaining({
        entries: expect.arrayContaining([expectedEntry]),
      }),
      true,
    );
  });

  it('should add a new row to the form', () => {
    const onUpdate = jest.fn();

    render(<UrlWhiteListForm urls={config.entries}
                             disabled={config.disabled}
                             onUpdate={onUpdate} />);

    expect(screen.queryByRole('cell', { name: String(config.entries.length + 1) })).not.toBeInTheDocument();

    const button = screen.getAllByRole('button', { name: /add url/i })[0];

    expect(button).toBeInTheDocument();

    userEvent.click(button);

    expect(screen.getByRole('cell', { name: String(config.entries.length + 1) })).toBeInTheDocument();

    expect(onUpdate).toHaveBeenCalledTimes(2);
  });

  it('should delete a row', async () => {
    const onUpdate = jest.fn();

    render(<UrlWhiteListForm urls={config.entries}
                             disabled={config.disabled}
                             onUpdate={onUpdate} />);

    expect(screen.getByDisplayValue(config.entries[0].title)).toBeInTheDocument();

    const row = screen.getByRole('row', { name: /1/i, exact: true });
    const deleteButton = within(row).getByRole('button', { name: /delete entry/i });

    expect(deleteButton).toBeInTheDocument();

    userEvent.click(deleteButton);

    await waitFor(() => expect(onUpdate).toHaveBeenCalledTimes(2));

    expect(screen.queryByDisplayValue(config.entries[0].title)).not.toBeInTheDocument();

    expect(onUpdate).toHaveBeenLastCalledWith(
      expect.objectContaining({
        entries: config.entries.filter(({ id }) => id !== config.entries[0].id),
      }),
      true,
    );
  });

  describe('validates entries', () => {
    it('should call api to validate regex url', async () => {
      const onUpdate = jest.fn();
      const nextValue = 'https://graylog.org';

      render(<UrlWhiteListForm urls={config.entries}
                               disabled={config.disabled}
                               onUpdate={onUpdate} />);

      const urlInput = screen.getByDisplayValue(config.entries[0].value);

      userEvent.clear(urlInput);
      userEvent.type(urlInput, nextValue);

      const numberCalls = 2; // First render + debounce
      await waitFor(() => expect(onUpdate).toHaveBeenCalledTimes(numberCalls));

      expect(mockTestRegexValidity).toHaveBeenCalledTimes(numberCalls - 1);

      const expectedEntry = { ...config.entries[0], value: nextValue };

      expect(onUpdate).toHaveBeenLastCalledWith(
        expect.objectContaining({
          entries: expect.arrayContaining([expectedEntry]),
        }),
        true,
      );
    });

    it('should validate title', async () => {
      const onUpdate = jest.fn();

      render(<UrlWhiteListForm urls={config.entries}
                               onUpdate={onUpdate} />);

      expect(onUpdate).toHaveBeenCalledTimes(1);
      expect(onUpdate).toHaveBeenLastCalledWith(expect.any(Object), true);

      const titleInput = screen.getByDisplayValue(config.entries[2].title);

      expect(titleInput).toBeInTheDocument();

      userEvent.clear(titleInput);

      await screen.findByText(/Required field/i);

      expect(onUpdate).toHaveBeenCalledTimes(2);
      expect(onUpdate).toHaveBeenLastCalledWith(expect.any(Object), false);
    });

    it('should validate entry after type changes', async () => {
      const onUpdate = jest.fn();
      mockTestRegexValidity.mockImplementationOnce(() => Promise.resolve({ is_valid: false }));

      render(<UrlWhiteListForm urls={config.entries}
                               disabled={config.disabled}
                               onUpdate={onUpdate} />);

      const row = screen.getByRole('row', { name: /3/i, exact: true });
      const select = within(row).getByText(/exact match/i);

      await selectEvent.openMenu(select);
      await selectEvent.select(select, 'Regex');

      await screen.findByText(/not a valid java regular expression/i);

      expect(mockTestRegexValidity).toHaveBeenCalledTimes(1);

      const expectedEntry = { ...config.entries[0], type: 'regex' };

      expect(onUpdate).toHaveBeenLastCalledWith(
        expect.objectContaining({
          entries: expect.arrayContaining([expectedEntry]),
        }),
        false,
      );
    });

    it('should validate new entry on first render', async () => {
      const entries: Array<Url> = [
        {
          id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
          title: 'A valid URL',
          value: 'https://graylog.org',
          type: 'literal',
        },
        {
          id: 'f7033f1f-d50f-4323-96df-294ede41d312',
          title: '',
          value: 'http://',
          type: 'literal',
        },
      ];

      const onUpdate = jest.fn();

      render(<UrlWhiteListForm urls={entries}
                               onUpdate={onUpdate}
                               newEntryId="f7033f1f-d50f-4323-96df-294ede41d312" />);

      await screen.findByText(/required field/i);
      await screen.findByText(/not a valid url/i);

      expect(mockTestRegexValidity).not.toHaveBeenCalled();
      expect(onUpdate).toHaveBeenCalledTimes(2);
      expect(onUpdate).toHaveBeenLastCalledWith(expect.any(Object), false);
    });
  });
});
