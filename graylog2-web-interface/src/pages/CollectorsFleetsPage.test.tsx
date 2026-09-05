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

import { asMock } from 'helpers/mocking';
import { useCollectorsConfig } from 'components/collectors/hooks';
import type { CollectorsConfig } from 'components/collectors/types';

import CollectorsFleetsPage from './CollectorsFleetsPage';

jest.mock('components/collectors/hooks', () => ({
  useCollectorsConfig: jest.fn(),
}));
jest.mock('components/collectors/fleets/CollectorsFleets', () => () => <div>fleets content</div>);
jest.mock('components/collectors/common', () => ({ CollectorsPageNavigation: () => <div>collectors nav</div> }));
jest.mock('components/common/CreateButton', () => () => null);

const navigateTo = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  Navigate: ({ to }: { to: string }) => {
    navigateTo(to);

    return <div>redirected</div>;
  },
}));

describe('CollectorsFleetsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('redirects to the overview wizard when collectors are not configured', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: null } as unknown as CollectorsConfig,
      isLoading: false,
    });

    render(<CollectorsFleetsPage />);

    expect(navigateTo).toHaveBeenCalledWith('/system/collectors');
    expect(screen.queryByText('fleets content')).not.toBeInTheDocument();
  });

  it('renders the page once collectors are configured', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: 'signing-id' } as unknown as CollectorsConfig,
      isLoading: false,
    });

    render(<CollectorsFleetsPage />);

    expect(screen.getByText('fleets content')).toBeInTheDocument();
    expect(navigateTo).not.toHaveBeenCalled();
  });
});
