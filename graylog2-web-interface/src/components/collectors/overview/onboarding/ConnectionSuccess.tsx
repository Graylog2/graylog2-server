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
import styled, { css } from 'styled-components';

import { Alert, Label } from 'components/bootstrap';
import { AccessibleCard } from 'components/common';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

import type { PlatformId } from './platforms';
import PLATFORMS from './platforms';

import StatCard from '../../common/StatCard';

type Props = {
  platformId: PlatformId;
};

const MOCK_CONNECTION = {
  hostname: 'web-prod-01',
  version: 'v1.0.0',
  fleetName: 'Default Fleet',
  sources: ['syslog', 'auth.log'],
  assets: [
    { type: 'host', name: 'web-prod-01' },
    { type: 'user', name: 'root' },
  ],
  messageCount: 142,
};

const SummaryRow = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.sm};
    flex-wrap: wrap;
    margin-bottom: ${theme.spacings.lg};
  `,
);

const StatsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
    margin-bottom: ${theme.spacings.lg};
  `,
);

const SectionTitle = styled.h3(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h3};
    margin: 0 0 ${theme.spacings.sm} 0;
  `,
);

const AssetsGrid = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
    margin-bottom: ${theme.spacings.lg};
  `,
);

const AssetCard = styled(AccessibleCard)(
  ({ theme }) => css`
    min-width: 150px;
    padding: ${theme.spacings.md};
  `,
);

const AssetType = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.gray[60]};
    text-transform: uppercase;
    margin-bottom: ${theme.spacings.xxs};
  `,
);

const AssetName = styled.div`
  font-weight: 500;
`;

const NextGrid = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
  `,
);

const NextCard = styled(AccessibleCard)(
  ({ theme }) => css`
    flex: 1;
    min-width: 180px;
    padding: ${theme.spacings.md};

    h4 {
      margin: 0 0 ${theme.spacings.xxs} 0;
      font-size: ${theme.fonts.size.large};
    }

    p {
      margin: 0;
      font-size: ${theme.fonts.size.small};
      color: ${theme.colors.gray[60]};
    }
  `,
);

const ConnectionSuccess = ({ platformId }: Props) => {
  const history = useHistory();
  const platform = PLATFORMS.find((p) => p.id === platformId);

  return (
    <div>
      <Alert bsStyle="success">
        Collector connected &mdash; <strong>{MOCK_CONNECTION.hostname}</strong> running{' '}
        <strong>{MOCK_CONNECTION.version}</strong>
      </Alert>

      <SummaryRow>
        <Label>{MOCK_CONNECTION.fleetName}</Label>
        <Label>{platform?.label}</Label>
        <Label>{MOCK_CONNECTION.sources.length} sources</Label>
      </SummaryRow>

      <StatsRow>
        <StatCard value={1} label="Online" variant="success" />
        <StatCard value={MOCK_CONNECTION.messageCount} label="Messages" />
        <StatCard value={MOCK_CONNECTION.sources.length} label="Sources" />
      </StatsRow>

      <SectionTitle>Auto-detected assets</SectionTitle>
      <AssetsGrid>
        {MOCK_CONNECTION.assets.map((asset) => (
          <AssetCard key={`${asset.type}-${asset.name}`}>
            <AssetType>{asset.type}</AssetType>
            <AssetName>{asset.name}</AssetName>
          </AssetCard>
        ))}
      </AssetsGrid>

      <SectionTitle>What&apos;s next?</SectionTitle>
      <NextGrid>
        <NextCard onClick={() => history.push(Routes.SYSTEM.COLLECTORS.FLEETS)}>
          <h4>Manage Fleets</h4>
          <p>Group collectors by environment or team.</p>
        </NextCard>
        <NextCard onClick={() => history.push(Routes.SYSTEM.COLLECTORS.FLEETS)}>
          <h4>Configure Sources</h4>
          <p>Add file paths, journald, or Windows Event Log sources.</p>
        </NextCard>
        <NextCard onClick={() => history.push(Routes.SYSTEM.COLLECTORS.INSTANCES)}>
          <h4>View Instances</h4>
          <p>Monitor all connected collector instances.</p>
        </NextCard>
      </NextGrid>
    </div>
  );
};

export default ConnectionSuccess;
