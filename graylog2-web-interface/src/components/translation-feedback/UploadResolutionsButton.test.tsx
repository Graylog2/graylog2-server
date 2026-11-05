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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import useTableFetchContext from 'components/common/PaginatedEntityTable/useTableFetchContext';
import UserNotification from 'util/UserNotification';

import { applyTranslationFeedbackResolutions } from './api';
import UploadResolutionsButton from './UploadResolutionsButton';

jest.mock('./api', () => ({
  applyTranslationFeedbackResolutions: jest.fn(() => Promise.resolve({ changed: 2, not_found: 1 })),
}));
jest.mock('components/common/PaginatedEntityTable/useTableFetchContext');
jest.mock('util/UserNotification', () => ({ success: jest.fn(), error: jest.fn() }));

const upload = async (contents: string) => {
  render(<UploadResolutionsButton />);

  await userEvent.upload(
    screen.getByTestId('upload-resolutions-input'),
    new File([contents], 'resolutions.json', { type: 'application/json' }),
  );
};

describe('UploadResolutionsButton', () => {
  const refetch = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest
      .mocked(useTableFetchContext)
      .mockReturnValue({ refetch, attributes: [], entityTableId: 'translation-feedback' });
  });

  it('applies the resolutions of the uploaded file', async () => {
    const resolutions = {
      resolutions: [
        { id: 'a', status: 'fixed', note: 'Changed' },
        { id: 'b', status: 'wont_fix' },
      ],
    };

    await upload(JSON.stringify(resolutions));

    await waitFor(() => expect(applyTranslationFeedbackResolutions).toHaveBeenCalledWith(resolutions));
    await waitFor(() => expect(refetch).toHaveBeenCalled());
    expect(UserNotification.success).toHaveBeenCalledWith('2 reports updated, 1 not found.', 'Resolutions applied');
  });

  it('rejects files without resolutions', async () => {
    await upload('{"something": "else"}');

    await waitFor(() =>
      expect(UserNotification.error).toHaveBeenCalledWith(
        'Applying the resolutions failed: The file does not contain a list of resolutions.',
        'Could not apply resolutions',
      ),
    );
    expect(applyTranslationFeedbackResolutions).not.toHaveBeenCalled();
  });
});
