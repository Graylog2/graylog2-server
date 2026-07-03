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

import { BrandIcon, Card, Icon } from 'components/common';

type DataSource =
  | { type: 'brand'; name: 'google' | 'aws' | 'microsoft' | 'paloalto'; label: string }
  | { type: 'material'; name: 'dns'; label: string };

const DATA_SOURCES: Array<DataSource> = [
  { type: 'brand', name: 'google', label: 'Google' },
  { type: 'brand', name: 'aws', label: 'AWS' },
  { type: 'brand', name: 'microsoft', label: 'Microsoft' },
  { type: 'material', name: 'dns', label: 'Syslog' },
  { type: 'brand', name: 'paloalto', label: 'Palo Alto' },
];

const Sources = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.md};
    align-items: center;
  `,
);

const SourceCard = styled(Card)`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const IconContainer = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;

  /* BrandIcon has its own 20x20 container — scale it to match */
  > div {
    width: 24px;
    height: 24px;

    svg {
      width: 24px;
      height: 24px;
    }
  }
`;

const MaterialIcon = styled(Icon)`
  && {
    font-size: 24px;
  }
`;

const AndMore = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const renderIcon = (source: DataSource) =>
  source.type === 'brand' ? <BrandIcon name={source.name} /> : <MaterialIcon name={source.name} />;

// Static, non-interactive list of example data-source icons for the first-use welcome page.
const DataSourceIcons = () => (
  <Sources>
    {DATA_SOURCES.map((source) => (
      <SourceCard key={source.label} padding="sm">
        <IconContainer title={source.label}>{renderIcon(source)}</IconContainer>
      </SourceCard>
    ))}
    <AndMore>And more</AndMore>
  </Sources>
);

export default DataSourceIcons;
