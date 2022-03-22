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
import { screen, render, act, within, waitFor } from 'wrappedTestingLibrary';

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
  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  describe('render the UrlWhitelistForm component', () => {
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
      jest.useFakeTimers();

      const onUpdate = jest.fn();

      render(<UrlWhiteListForm urls={config.entries}
                               disabled={config.disabled}
                               onUpdate={onUpdate} />);

      expect(screen.queryByDisplayValue(/foobar/i)).not.toBeInTheDocument();

      const titleInput = screen.getByDisplayValue(config.entries[0].title);

      userEvent.clear(titleInput);
      userEvent.type(titleInput, 'foobar');

      // Regex entries are debounced
      act(() => {
        jest.runAllTimers();
      });

      expect(onUpdate).toHaveBeenCalledTimes(2);

      const expectedEntry = { ...config.entries[0], title: 'foobar' };

      expect(onUpdate).toHaveBeenLastCalledWith(
        expect.objectContaining({
          entries: expect.arrayContaining([expectedEntry]),
        }),
        true,
      );
    });

    it('should call api to check regex value', async () => {
      jest.useFakeTimers();

      const onUpdate = jest.fn();

      render(<UrlWhiteListForm urls={config.entries}
                               disabled={config.disabled}
                               onUpdate={onUpdate} />);

      const urlInput = screen.getByDisplayValue(config.entries[0].value);

      userEvent.clear(urlInput);
      userEvent.type(urlInput, 'https://graylog.org');

      // Regex entries are debounced
      act(() => {
        jest.runAllTimers();
      });

      await waitFor(() => expect(mockTestRegexValidity).toHaveBeenCalledTimes(1));

      expect(onUpdate).toHaveBeenCalledTimes(2);

      const expectedEntry = { ...config.entries[0], value: 'https://graylog.org' };

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

      expect(screen.queryByDisplayValue(config.entries[0].title)).not.toBeInTheDocument();

      await waitFor(() => expect(onUpdate).toHaveBeenCalledTimes(2));

      expect(onUpdate).toHaveBeenLastCalledWith(
        expect.objectContaining({
          entries: config.entries.filter(({ id }) => id !== config.entries[0].id),
        }),
        true,
      );
    });

    it('should validate entries on change', async () => {
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
  });
});
