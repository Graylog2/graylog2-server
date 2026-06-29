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

import InstallCommand from './InstallCommand';

jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);

describe('InstallCommand', () => {
  const command = 'curl -fsSL https://graylog.example:4317/collectors/install | ENROLLMENT_TOKEN=abc123 bash';

  it('renders the install command text', () => {
    render(<InstallCommand command={command} platformLabel="Linux" tokenDuration="P1D" />);

    expect(screen.getByText(command)).toBeInTheDocument();
  });

  it('renders the copy button', () => {
    render(<InstallCommand command={command} platformLabel="Linux" tokenDuration="P1D" />);

    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument();
  });

  it('displays the token expiry note', () => {
    render(<InstallCommand command={command} platformLabel="Linux" tokenDuration="P1D" />);

    expect(screen.getByText(/token expires in 1 day/i)).toBeInTheDocument();
  });
});
