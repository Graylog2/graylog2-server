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

import Markdown from './Markdown';

describe('Markdown', () => {
  // We are skipping this test, until we can implement a headless theme provider from @mantine/core
  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('renders `undefined`', () => {
    const { container } = render(<Markdown text={undefined} />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  // We are skipping this test, until we can implement a headless theme provider from @mantine/core
  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('renders empty string', () => {
    const { container } = render(<Markdown text="" />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  it('renders simple markdown', async () => {
    render(<Markdown text="# Title" />);

    await screen.findByRole('heading', { name: /title/i });
  });
});
