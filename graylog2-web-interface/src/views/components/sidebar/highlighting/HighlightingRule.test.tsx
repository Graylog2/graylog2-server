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

import HighlightingRule from './HighlightingRule';

jest.mock('stores/useAppDispatch');

jest.mock('views/logic/slices/highlightActions', () => ({
  updateHighlightingRule: jest.fn(() => Promise.resolve()),
  removeHighlightingRule: jest.fn(() => Promise.resolve()),
}));

describe('HighlightingRule', () => {
  useViewsPlugin();

  const rule = Rule.create('response_time', '250', undefined, StaticColor.create('#f44242'));

  const SUT = (props: Partial<React.ComponentProps<typeof HighlightingRule>>) => (
    <HighlightingRule rule={rule}
                      onUpdate={() => Promise.resolve()}
                      onDelete={() => Promise.resolve()}
                      {...props} />
  );

  it('should display field and value of rule', async () => {
    render(<SUT />);

    await screen.findByText('response_time');
    await screen.findByText(/250/);
  });

  it('should update rule if color was changed', async () => {
    const onUpdate = jest.fn(() => Promise.resolve());
    render(<SUT onUpdate={onUpdate} />);

    const staticColorPicker = await screen.findByTestId('static-color-preview');
    userEvent.click(staticColorPicker);
    userEvent.click(await screen.findByTitle(/#fbfdd8/i));

    await waitFor(() => {
      expect(onUpdate).toHaveBeenCalledWith(rule, rule.field, rule.value, rule.condition, StaticColor.create('#fbfdd8'));
    });
  });

  it('should close popover when color was changed', async () => {
    render(<SUT />);

    const staticColorPicker = await screen.findByTestId('static-color-preview');
    userEvent.click(staticColorPicker);

    userEvent.click(await screen.findByTitle(/#fbfdd8/i));

    await waitFor(() => {
      expect(screen.queryByTitle(/#ff51b9/i)).not.toBeInTheDocument();
    });
  });

  describe('rule edit', () => {
    it('should show a edit modal', async () => {
      render(<SUT />);
      const editIcon = await screen.findByTitle('Edit this Highlighting Rule');

      expect(screen.queryByText('Edit Highlighting Rule')).not.toBeInTheDocument();

      userEvent.click(editIcon);

      await screen.findByText('Edit Highlighting Rule');
    });
  });

  describe('rule removal:', () => {
    let oldConfirm = null;
    const findDeleteIcon = () => screen.findByTitle('Remove this Highlighting Rule');

    beforeEach(async () => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => false);
    });

    afterEach(() => {
      window.confirm = oldConfirm;
    });

    it('asks for confirmation before rule is removed', async () => {
      render(<SUT />);
      userEvent.click(await findDeleteIcon());

      expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove this highlighting?');
    });

    it('does not remove rule if confirmation was cancelled', async () => {
      render(<SUT />);
      userEvent.click(await findDeleteIcon());

      await screen.findByText('response_time');
    });

    it('removes rule rule if confirmation was acknowledged', async () => {
      const onDelete = jest.fn(() => Promise.resolve());
      render(<SUT onDelete={onDelete} />);
      window.confirm = jest.fn(() => true);
      userEvent.click(await findDeleteIcon());

      await waitFor(() => expect(onDelete).toHaveBeenCalledWith(rule));
    });
  });
});
