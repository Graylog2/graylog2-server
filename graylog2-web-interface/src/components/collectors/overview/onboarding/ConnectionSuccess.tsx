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
import type { CollectorInstanceView } from 'components/collectors/types';
import { useSources } from 'components/collectors/hooks/useSourceQueries';

import type { PlatformId } from './platforms';
import PLATFORMS from './platforms';
import useCollectorLogPreview from './useCollectorLogPreview';
import LogPreviewSection from './LogPreviewSection';

import StatCard from '../../common/StatCard';
import collectorReceivedMessagesUrl from '../../common/collectorReceivedMessagesUrl';
import collectorSystemLogsUrl from '../../common/collectorSystemLogsUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from '../../common/fields';

type Props = {
  platformId?: PlatformId;
  instance: CollectorInstanceView;
  fleetName: string | undefined;
};

// Asset auto-detection has no backend yet — intentionally stays mocked until that feature ships.
const MOCK_ASSETS = [
  { type: 'host', name: 'example-host' },
  { type: 'user', name: 'root' },
];

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

const LogPreviewsWrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
  `,
);

const ConnectionSuccess = ({ platformId = undefined, instance, fleetName }: Props) => {
  const history = useHistory();
  const platform = PLATFORMS.find((p) => p.id === platformId);
  const { selfLogs, sourceLogs, selfLogsError, sourceLogsError, isLoading } = useCollectorLogPreview(
    instance.instance_uid,
  );
  const { data: sources } = useSources(instance.fleet_id);

  return (
    <div>
      <Alert bsStyle="success">
        Collector connected &mdash; <strong>{instance.hostname ?? instance.instance_uid}</strong>
        {instance.version && (
          <>
            {' '}
            running <strong>v{instance.version}</strong>
          </>
        )}
      </Alert>

      <SummaryRow>
        {fleetName && <Label>{fleetName}</Label>}
        {platform && <Label>{platform.label}</Label>}
        <Label>{sources?.length ?? 0} sources</Label>
      </SummaryRow>

      <StatsRow>
        <StatCard value={instance.status === 'online' ? 1 : 0} label="Online" variant="success" />
        <StatCard value={sourceLogs?.total ?? 0} label="Messages (15 min)" />
        <StatCard value={sources?.length ?? 0} label="Sources" />
      </StatsRow>

      <LogPreviewsWrapper>
        <LogPreviewSection
          title="Your log sources"
          searchUrl={collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid)}
          preview={sourceLogs}
          isLoading={isLoading}
          error={sourceLogsError}
        />

        <LogPreviewSection
          title="Collector logs"
          searchUrl={collectorSystemLogsUrl(instance.instance_uid)}
          preview={selfLogs}
          isLoading={isLoading}
          error={selfLogsError}
          collapsible
        />
      </LogPreviewsWrapper>

      <SectionTitle>Auto-detected assets</SectionTitle>
      <AssetsGrid>
        {MOCK_ASSETS.map((asset) => (
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
        <NextCard onClick={() => history.push(Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id))}>
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
