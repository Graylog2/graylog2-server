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
import useDismissOnboarding from 'components/welcome/hooks/useDismissOnboarding';
import AppConfig from 'util/AppConfig';

import FirstUseWelcome from './FirstUseWelcome';

jest.mock('components/welcome/hooks/useDismissOnboarding');

const mockDismiss = jest.fn();

beforeEach(() => {
  asMock(useDismissOnboarding).mockReturnValue({ mutate: mockDismiss } as unknown as ReturnType<
    typeof useDismissOnboarding
  >);
  asMock(AppConfig.isCloud).mockReturnValue(false);
});

describe('FirstUseWelcome', () => {
  it('links the "Set up Collector" button to the collectors overview', async () => {
    render(<FirstUseWelcome />);

    const link = await screen.findByRole('link', { name: /Set up Collector/i });

    expect(link).toHaveAttribute('href', '/system/collectors');
  });

  it('shows the supported platform icons', () => {
    render(<FirstUseWelcome />);

    expect(screen.getByTitle('Linux')).toBeInTheDocument();
    expect(screen.getByTitle('Windows')).toBeInTheDocument();
  });

  it('links the "Configure Input" button to the inputs page', async () => {
    render(<FirstUseWelcome />);

    const link = await screen.findByRole('link', { name: /Configure Input/i });

    expect(link).toHaveAttribute('href', '/system/inputs');
  });

  it('shows the data source icons', () => {
    render(<FirstUseWelcome />);

    expect(screen.getByTitle('Google')).toBeInTheDocument();
    expect(screen.getByTitle('AWS')).toBeInTheDocument();
  });

  it('renders resource panels with a continue link that opens in a new tab', () => {
    render(<FirstUseWelcome />);

    const links = screen.getAllByRole('link', { name: /continue/i });

    expect(links[0]).toHaveAttribute('href', 'https://www.graylog.org');
    expect(links[0]).toHaveAttribute('target', '_blank');
  });

  it('asks for confirmation before dismissing the onboarding', async () => {
    render(<FirstUseWelcome />);

    await userEvent.click(screen.getByRole('button', { name: /dismiss onboarding/i }));

    expect(await screen.findByRole('button', { name: /Dismiss for everyone/i })).toBeInTheDocument();
    expect(mockDismiss).not.toHaveBeenCalled();
  });

  it('dismisses the onboarding after confirming', async () => {
    render(<FirstUseWelcome />);

    await userEvent.click(screen.getByRole('button', { name: /dismiss onboarding/i }));
    await userEvent.click(await screen.findByRole('button', { name: /Dismiss for everyone/i }));

    expect(mockDismiss).toHaveBeenCalledTimes(1);
  });

  it('shows the "Set up Other Sources" section when not on cloud', () => {
    render(<FirstUseWelcome />);

    expect(screen.getByRole('heading', { name: /set up other sources/i })).toBeInTheDocument();
  });

  it('hides the "Set up Other Sources" section on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);
    render(<FirstUseWelcome />);

    expect(screen.queryByRole('heading', { name: /set up other sources/i })).not.toBeInTheDocument();
  });
});
