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
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'wrappedTestingLibrary';

import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import HealthModule from './HealthModule';
import useHealthModuleVisible from './useHealthModuleVisible';

jest.mock('./useHealthModuleVisible');
jest.mock('hooks/usePluggableLicenseCheck');

const mockedUseHealthModuleVisible = jest.mocked(useHealthModuleVisible);
const mockedUsePluggableLicenseCheck = jest.mocked(usePluggableLicenseCheck);

const licenseCheckResult = (valid: boolean): ReturnType<typeof usePluggableLicenseCheck> => ({
  data: { valid, expired: false, violated: false },
  isInitialLoading: false,
  refetch: () => {},
});

const clickInTree = async (label: string) => {
  const tree = screen.getByLabelText('Cluster health tree');

  await userEvent.click(within(tree).getByText(label));
};

describe('HealthModule', () => {
  beforeEach(() => {
    mockedUseHealthModuleVisible.mockReturnValue(true);
    mockedUsePluggableLicenseCheck.mockReturnValue(licenseCheckResult(true));
  });

  it('renders the interpretation legend by default when the synthetic root is selected', () => {
    render(<HealthModule />);

    expect(screen.getByRole('heading', { name: 'Health of Graylog Deployment' })).toBeInTheDocument();
    expect(screen.getByLabelText('Cluster health tree')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'How to interpret this health report:' })).toBeInTheDocument();

    expect(screen.getByText(/Functioning properly/)).toBeInTheDocument();
    expect(screen.getByText(/Experiencing a problem/)).toBeInTheDocument();
    expect(screen.getByText(/Severe issues/)).toBeInTheDocument();
    expect(screen.getByText(/The state could not be evaluated/)).toBeInTheDocument();
  });

  it('renders Affected list with non-healthy children when a feature is selected', async () => {
    render(<HealthModule />);

    await clickInTree('MongoDB');

    expect(screen.getByRole('heading', { name: 'MongoDB' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Affected' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'How to interpret this health report:' })).not.toBeInTheDocument();
  });

  it('renders the full check panel when a non-healthy leaf is selected', async () => {
    render(<HealthModule />);

    await clickInTree('Graylog');
    await clickInTree('Memory');

    expect(screen.getByRole('heading', { name: 'Memory' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'What this means' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Common causes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Recommended action' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Affected' })).not.toBeInTheDocument();
  });

  it('renders the latest backend message instead of common causes when one is present', async () => {
    render(<HealthModule />);

    await clickInTree('MongoDB');
    await clickInTree('Primary State');

    expect(screen.getByRole('heading', { name: 'Primary State' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Latest message' })).toBeInTheDocument();
    expect(screen.getByText('Replica set has no primary; 2 of 3 voting members are unreachable.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Common causes' })).not.toBeInTheDocument();
  });

  it('renders the entity-list button with the configured label', async () => {
    render(<HealthModule />);

    await clickInTree('MongoDB');
    await clickInTree('Connectivity');

    const recommendedActionHeading = screen.getByRole('heading', { name: 'Recommended action' });
    const entityListLink = screen.getByRole('link', { name: /View MongoDB/i });
    const latestMessageHeading = screen.getByRole('heading', { name: 'Latest message' });

    expect(
      recommendedActionHeading.compareDocumentPosition(entityListLink) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(
      entityListLink.compareDocumentPosition(latestMessageHeading) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('cascades expansion to non-healthy descendants when an unhealthy feature is opened', async () => {
    render(<HealthModule />);

    const tree = screen.getByLabelText('Cluster health tree');

    expect(within(tree).queryByText('Memory')).not.toBeInTheDocument();

    await clickInTree('Graylog');

    expect(within(tree).getByText('Server')).toBeInTheDocument();
    expect(within(tree).getByText('Memory')).toBeInTheDocument();
  });

  it('cascades collapse to all descendants when a feature is closed', async () => {
    render(<HealthModule />);

    const tree = screen.getByLabelText('Cluster health tree');

    await clickInTree('Graylog');
    expect(within(tree).getByText('Memory')).toBeInTheDocument();

    await clickInTree('Graylog');

    expect(within(tree).queryByText('Memory')).not.toBeInTheDocument();
    expect(within(tree).queryByText('Server')).not.toBeInTheDocument();
  });

  it('keeps the synthetic root expanded even when toggled', async () => {
    render(<HealthModule />);

    await clickInTree('Cluster Health');

    const tree = screen.getByLabelText('Cluster health tree');

    expect(within(tree).getByText('Graylog')).toBeInTheDocument();
    expect(within(tree).getByText('MongoDB')).toBeInTheDocument();
  });

  it('does not render when the visibility flag is off', () => {
    mockedUseHealthModuleVisible.mockReturnValue(false);

    render(<HealthModule />);

    expect(screen.queryByRole('heading', { name: 'Health of Graylog Deployment' })).not.toBeInTheDocument();
  });

  it('does not render when no enterprise license is present', () => {
    mockedUsePluggableLicenseCheck.mockReturnValue(licenseCheckResult(false));

    render(<HealthModule />);

    expect(screen.queryByRole('heading', { name: 'Health of Graylog Deployment' })).not.toBeInTheDocument();
  });
});
