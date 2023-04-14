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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useClusterSupportBundle from 'hooks/useClusterSupportBundle';

import ClusterSupportBundleOverview from './ClusterSupportBundleOverview';

jest.mock('hooks/useClusterSupportBundle');

const onCreate = jest.fn();
const onDelete = jest.fn();
const onDownload = jest.fn();

const file_name = 'bundle1';

describe('ClusterSupportBundleOverview', () => {
  beforeAll(() => {
    asMock(useClusterSupportBundle).mockImplementation(
      () => ({
        isCreating: false,
        list: [{ size: 1, file_name }],
        onCreate,
        onDelete,
        onDownload,
      }),
    );
  });

  it('Should create bundle', async () => {
    render(<ClusterSupportBundleOverview />);

    const createButton = screen.getByRole('button', { name: /Create Support Bundle/i });

    fireEvent.click(createButton);

    expect(onCreate).toHaveBeenCalled();
  });

  it('Should delete bundle', async () => {
    render(<ClusterSupportBundleOverview />);

    const deleteButton = screen.getByRole('button', { name: /Delete/i });

    fireEvent.click(deleteButton);

    const confirmButton = screen.getByRole('button', { name: /Confirm/i, hidden: true });

    expect(confirmButton).toBeInTheDocument();

    fireEvent.click(confirmButton);

    expect(onDelete).toHaveBeenCalledWith(file_name);
  });

  it('Should download bundle', async () => {
    render(<ClusterSupportBundleOverview />);

    const downloadButton = screen.getByRole('button', { name: /Download/i });

    fireEvent.click(downloadButton);

    expect(onDownload).toHaveBeenCalledWith(file_name);
  });
});
