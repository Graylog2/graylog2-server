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

import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import { createContentPack } from 'hooks/useContentPackMutations';
import UserNotification from 'util/UserNotification';
import useHistory from 'routing/useHistory';
import mockHistory from 'helpers/mocking/mockHistory';
import asMock from 'helpers/mocking/AsMock';
import Routes from 'routing/Routes';

jest.mock('hooks/useContentPackMutations', () => ({
  createContentPack: jest.fn(),
}));

jest.mock('util/UserNotification', () => ({
  success: jest.fn(),
  error: jest.fn(),
}));

jest.mock('routing/useHistory');

const uploadFile = async (contents: string) => {
  const file = new File([contents], 'content-pack.json', { type: 'application/json' });

  await userEvent.click(await screen.findByRole('button', { name: /upload/i }));

  const fileInput = await screen.findByLabelText(/choose file/i);
  await userEvent.upload(fileInput, file);

  // The trigger and the modal submit button share the "Upload" label — submit the last one (the modal button).
  const uploadButtons = screen.getAllByRole('button', { name: /^upload$/i });
  await userEvent.click(uploadButtons[uploadButtons.length - 1]);
};

describe('<ContentPackUploadControls />', () => {
  const history = mockHistory();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useHistory).mockReturnValue(history);
  });

  it('should render', async () => {
    render(<ContentPackUploadControls />);

    await screen.findByRole('button', { name: /upload/i });
  });

  it('shows a success notification including the pack name and redirects to its details page', async () => {
    asMock(createContentPack).mockResolvedValue({ id: 'pack-id-1', rev: 1, name: 'My Content Pack' });
    render(<ContentPackUploadControls />);

    await uploadFile('{ "v": 1 }');

    await waitFor(() =>
      expect(UserNotification.success).toHaveBeenCalledWith(
        'Content pack "My Content Pack" imported successfully',
        'Success!',
      ),
    );

    expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.CONTENTPACKS.show('pack-id-1'));
  });
});
