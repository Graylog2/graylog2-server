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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import useRightSidebar from 'hooks/useRightSidebar';

import MarkdownLink from './MarkdownLink';

jest.mock('hooks/usePluginEntities');
jest.mock('hooks/useRightSidebar');

describe('MarkdownLink', () => {
  const openSidebar = jest.fn();
  const onClick = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useRightSidebar).mockReturnValue({ openSidebar } as unknown as ReturnType<typeof useRightSidebar>);
    asMock(usePluginEntities).mockReturnValue([{ uriSegment: 'investigations', resolve: () => ({ onClick }) }]);
  });

  it('opens the sidebar without exposing the internal URI as an href', async () => {
    render(<MarkdownLink href="graylog:///investigations/5f4dcc3b5aa765d61d8327de">Investigation</MarkdownLink>);

    const link = screen.getByRole('link', { name: 'Investigation' });

    expect(link).not.toHaveAttribute('href');

    await userEvent.click(link);

    expect(onClick).toHaveBeenCalledWith({ openSidebar });
  });

  it('opens the sidebar when the link is activated by keyboard', async () => {
    render(<MarkdownLink href="graylog:///investigations/5f4dcc3b5aa765d61d8327de">Investigation</MarkdownLink>);

    screen.getByRole('link', { name: 'Investigation' }).focus();
    await userEvent.keyboard('{Enter}');

    expect(onClick).toHaveBeenCalledWith({ openSidebar });
  });

  it('keeps the href for a link no resolver claims', () => {
    render(<MarkdownLink href="https://graylog.org">Docs</MarkdownLink>);

    expect(screen.getByRole('link', { name: /Docs/ })).toHaveAttribute('href', 'https://graylog.org');
  });
});
