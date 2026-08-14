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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { Link, RelativeTime, SimpleGrid } from 'components/common';
import Routes from 'routing/Routes';
import { defaultCompare } from 'logic/DefaultCompare';
import type { CollectorInstanceView } from 'components/collectors/types';

import QuietSection from './QuietSection';

import collectorOsName from '../../common/collectorOsName';

type Props = {
  instance: CollectorInstanceView;
  fleetName: string | undefined;
};

const FactLabel = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.tiny};
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: ${theme.colors.gray[60]};
  `,
);

const FactValue = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    font-weight: 600;
    overflow-wrap: anywhere;
  `,
);

const StatusDot = styled.span<{ $online: boolean }>(
  ({ $online, theme }) => css`
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: ${theme.spacings.xxs};
    background-color: ${$online ? theme.colors.variant.success : theme.colors.variant.warning};
  `,
);

const ToggleButton = styled(Button)`
  padding-left: 0;
`;

const Fact = ({ label, children = undefined }: React.PropsWithChildren<{ label: string }>) => (
  <div>
    <FactLabel>{label}</FactLabel>
    <FactValue>{children}</FactValue>
  </div>
);

/**
 * The curated collector facts (host, OS, version, fleet, enrollment and heartbeat times) with the
 * full attribute list tucked behind a toggle — the handful of values that matter during onboarding
 * stay scannable while everything the agent reported remains one click away.
 */
const CollectorFactsSection = ({ instance, fleetName }: Props) => {
  const [showAttributes, setShowAttributes] = useState(false);

  const attributes = [
    ...Object.entries(instance.identifying_attributes ?? {}),
    ...Object.entries(instance.non_identifying_attributes ?? {}),
  ].sort((attr1, attr2) => defaultCompare(attr1[0], attr2[0]));

  return (
    <QuietSection title="Collector" titleAs="h3">
      <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md" verticalSpacing="sm">
        <Fact label="Host">{instance.hostname ?? instance.instance_uid}</Fact>
        <Fact label="OS">{collectorOsName(instance, true)}</Fact>
        <Fact label="Version">{instance.version ?? 'Unknown'}</Fact>
        <Fact label="Fleet">
          <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>{fleetName ?? 'Unknown'}</Link>
        </Fact>
        <Fact label="Enrolled">
          <RelativeTime dateTime={instance.enrolled_at} />
        </Fact>
        <Fact label="Last seen">
          <StatusDot $online={instance.status === 'online'} />
          <RelativeTime dateTime={instance.last_seen} />
        </Fact>
        {showAttributes &&
          attributes.map(([key, value]) => (
            <Fact key={key} label={key}>
              {String(value)}
            </Fact>
          ))}
      </SimpleGrid>
      {attributes.length > 0 && (
        <ToggleButton bsStyle="link" bsSize="xsmall" onClick={() => setShowAttributes((show) => !show)}>
          {showAttributes ? 'Hide attributes' : `Show all ${attributes.length} attributes`}
        </ToggleButton>
      )}
    </QuietSection>
  );
};

export default CollectorFactsSection;
