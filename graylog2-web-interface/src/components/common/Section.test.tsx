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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import Section from './Section';

describe('Section', () => {
  it('should render children', async () => {
    render(<Section title="The Title">The children</Section>);

    await screen.findByRole('heading', { name: /the title/i });
    await screen.findByText(/the children/i);
  });

  it('should render actions', async () => {
    render(
      <Section title="The Title" actions="The actions">
        The children
      </Section>,
    );

    await screen.findByRole('heading', { name: /the title/i });
    await screen.findByText(/the children/i);
    await screen.findByText(/the actions/i);
  });

  it('should render headerLeftSection', async () => {
    render(
      <Section title="The Title" headerLeftSection="The left section">
        The children
      </Section>,
    );

    await screen.findByText(/the left section/i);
  });

  it('should render collapse button', async () => {
    render(
      <Section title="The Title" collapsible>
        The children
      </Section>,
    );

    await screen.findByRole('heading', { name: /the title/i });
    await screen.findByText(/the children/i);

    userEvent.click(screen.getByTestId('collapseButton'));

    const children = await screen.findByText(/the children/i);

    expect(children).not.toBeVisible();
  });

  it('should execute onCollapse', async () => {
    const onCollapse = jest.fn();

    render(
      <Section title="The Title" collapsible onCollapse={onCollapse}>
        The children
      </Section>,
    );

    await screen.findByRole('heading', { name: /the title/i });
    await screen.findByText(/the children/i);

    userEvent.click(screen.getByTestId('collapseButton'));

    const children = await screen.findByText(/the children/i);

    expect(children).not.toBeVisible();
    expect(onCollapse).toHaveBeenCalledWith(true);
  });
});
