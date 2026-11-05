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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { clearRenderedTranslations, recordTranslation } from 'util/i18n/renderedTranslations';

import { createTranslationFeedback } from './api';
import TranslationFeedbackContextMenu from './TranslationFeedbackContextMenu';

jest.mock('./api', () => ({ createTranslationFeedback: jest.fn(() => Promise.resolve({})) }));

const renderWithText = () =>
  render(
    <div>
      <span>Translated label</span>
      <span>Hardcoded label</span>
      <input aria-label="field" />
      <TranslationFeedbackContextMenu />
    </div>,
  );

describe('TranslationFeedbackContextMenu', () => {
  beforeEach(() => {
    clearRenderedTranslations();
    recordTranslation('Translated label', { namespace: 'search', key: 'label', usedLanguage: 'en' });
  });

  it('reports a translated text with its key', async () => {
    renderWithText();

    fireEvent.contextMenu(screen.getByText('Translated label'));
    await userEvent.click(await screen.findByRole('menuitem', { name: /report translation/i }));
    await userEvent.type(screen.getByLabelText(/suggested translation/i), 'Better label');
    await userEvent.click(screen.getByRole('button', { name: /^report$/i }));

    await waitFor(() =>
      expect(createTranslationFeedback).toHaveBeenCalledWith(
        expect.objectContaining({
          namespace: 'search',
          key: 'label',
          text: 'Translated label',
          suggestion: 'Better label',
          category: 'wrong',
        }),
      ),
    );
  });

  it('reports texts without translation without key', async () => {
    renderWithText();

    fireEvent.contextMenu(screen.getByText('Hardcoded label'));
    await userEvent.click(await screen.findByRole('menuitem', { name: /report translation/i }));

    expect(screen.getByText(/no translation found/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /^report$/i }));

    await waitFor(() =>
      expect(createTranslationFeedback).toHaveBeenCalledWith(
        expect.objectContaining({ namespace: undefined, key: undefined, text: 'Hardcoded label' }),
      ),
    );
  });

  it('keeps the browser menu for shift + right-click and form fields', () => {
    renderWithText();

    fireEvent.contextMenu(screen.getByText('Translated label'), { shiftKey: true });
    fireEvent.contextMenu(screen.getByLabelText('field'));

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });
});
