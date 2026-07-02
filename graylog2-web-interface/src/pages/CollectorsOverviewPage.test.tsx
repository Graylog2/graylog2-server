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

import CollectorsOverviewPage from './CollectorsOverviewPage';

jest.mock('components/collectors/hooks');
jest.mock('./CollectorsSettingsPage', () => () => <div>collectors settings page</div>);
jest.mock('components/collectors/overview', () => ({ CollectorsOverview: () => <div>collectors overview</div> }));
jest.mock('components/collectors/common', () => ({ CollectorsPageNavigation: () => <div>collectors nav</div> }));

describe('CollectorsOverviewPage', () => {
  it('renders the settings page instead of the overview when the config is missing', () => {
    asMock(useCollectorsConfig).mockReturnValue({ data: undefined, isLoading: false });

    render(<CollectorsOverviewPage />);

    expect(screen.getByText('collectors settings page')).toBeInTheDocument();
    expect(screen.queryByText('collectors overview')).not.toBeInTheDocument();
  });

  it('renders the overview once the config exists', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: 'signing-id' } as unknown as CollectorsConfig,
      isLoading: false,
    });

    render(<CollectorsOverviewPage />);

    expect(screen.getByText('collectors overview')).toBeInTheDocument();
    expect(screen.queryByText('collectors settings page')).not.toBeInTheDocument();
  });
});
