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

import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { asMock } from 'helpers/mocking';
import useAppDispatch from 'stores/useAppDispatch';
import mockDispatch from 'views/test/mockDispatch';
import { createSearch } from 'fixtures/searches';
import type { RootState } from 'views/types';
import { updateHighlightingRule, removeHighlightingRule } from 'views/logic/slices/highlightActions';

import HighlightingRule from './HighlightingRule';

jest.mock('stores/useAppDispatch');

jest.mock('views/logic/slices/highlightActions', () => ({
  updateHighlightingRule: jest.fn(() => Promise.resolve()),
  removeHighlightingRule: jest.fn(() => Promise.resolve()),
}));

describe('HighlightingRule', () => {
  useViewsPlugin();

  const rule = Rule.create('response_time', '250', undefined, StaticColor.create('#f44242'));
  const view = createSearch();
  const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);

  beforeEach(() => {
    asMock(useAppDispatch).mockReturnValue(dispatch);
  });

  it('should display field and value of rule', async () => {
    render(<HighlightingRule rule={rule} />);

    await screen.findByText('response_time');
    await screen.findByText(/250/);
  });

  it('should update rule if color was changed', async () => {
    render(<HighlightingRule rule={rule} />);

    const staticColorPicker = await screen.findByTestId('static-color-preview');
    userEvent.click(staticColorPicker);

    userEvent.click(await screen.findByTitle(/#fbfdd8/i));

    await waitFor(() => {
      expect(updateHighlightingRule).toHaveBeenCalledWith(rule, { color: StaticColor.create('#fbfdd8') });
    });
  });

  it('should close popover when color was changed', async () => {
    render(<HighlightingRule rule={rule} />);

    const staticColorPicker = await screen.findByTestId('static-color-preview');
    userEvent.click(staticColorPicker);

    userEvent.click(await screen.findByTitle(/#fbfdd8/i));

    await waitFor(() => {
      expect(screen.queryByTitle(/#ff51b9/i)).not.toBeInTheDocument();
    });
  });

  describe('rule edit', () => {
    it('should show a edit modal', async () => {
      render(<HighlightingRule rule={rule} />);
      const editIcon = await screen.findByTitle('Edit this Highlighting Rule');

      expect(screen.queryByText('Edit Highlighting Rule')).not.toBeInTheDocument();

      userEvent.click(editIcon);

      await screen.findByText('Edit Highlighting Rule');
    });
  });

  describe('rule removal:', () => {
    let oldConfirm = null;
    let deleteIcon;

    beforeEach(async () => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => false);

      // eslint-disable-next-line testing-library/no-render-in-setup
      render(<HighlightingRule rule={rule} />);

      deleteIcon = await screen.findByTitle('Remove this Highlighting Rule');
    });

    afterEach(() => {
      window.confirm = oldConfirm;
    });

    it('asks for confirmation before rule is removed', () => {
      userEvent.click(deleteIcon);

      expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove this highlighting?');
    });

    it('does not remove rule if confirmation was cancelled', async () => {
      userEvent.click(deleteIcon);

      await screen.findByText('response_time');
    });

    it('removes rule rule if confirmation was acknowledged', async () => {
      window.confirm = jest.fn(() => true);
      userEvent.click(deleteIcon);

      await waitFor(() => {
        expect(removeHighlightingRule).toHaveBeenCalledWith(rule);
      });
    });
  });
});
