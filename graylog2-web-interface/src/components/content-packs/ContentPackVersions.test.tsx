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

import 'helpers/mocking/react-dom_mock';
import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';

import ContentPackVersions from './ContentPackVersions';

describe('<ContentPackVersions />', () => {
  const contentPackRev = ContentPack.builder()
    .id('1')
    .name('UFW Grok Patterns')
    .description('Grok Patterns to extract informations from UFW logfiles')
    .summary('This is a summary')
    .vendor('graylog.com')
    .url('www.graylog.com');

  const contentPack = {
    1: contentPackRev.rev(1).build(),
    2: contentPackRev.rev(2).build(),
    3: contentPackRev.rev(3).build(),
    4: contentPackRev.rev(4).build(),
  };

  const contentPackRevision = new ContentPackRevisions(contentPack);

  it('should render with content pack versions', async () => {
    render(<ContentPackVersions contentPackRevisions={contentPackRevision} />);

    await screen.findByText('Select');
  });

  it('should fire on change when clicked on a version', async () => {
    const changeFn = jest.fn();
    render(<ContentPackVersions onChange={changeFn} contentPackRevisions={contentPackRevision} />);

    const inputs = await screen.findAllByRole('radio');
    userEvent.click(inputs[0]);

    await waitFor(() => {
      expect(changeFn).toHaveBeenCalledWith('1');
    });
  });

  it('should fire on delete when clicked on delete a version', async () => {
    const deleteFn = jest.fn();
    render(<ContentPackVersions onDeletePack={deleteFn} contentPackRevisions={contentPackRevision} />);

    const menuButton = await screen.findAllByRole('button', { name: /actions/i });
    userEvent.click(menuButton[0]);

    const deleteButton = await screen.findByRole('menuitem', { name: /delete/i });
    userEvent.click(deleteButton);

    await waitFor(() => {
      expect(deleteFn).toHaveBeenCalledWith('1', 1);
    });
  });
});
