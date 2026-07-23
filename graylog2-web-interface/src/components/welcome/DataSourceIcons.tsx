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

import { BrandIcon, Icon } from 'components/common';

import IconCard from './IconCard';

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

const MaterialIcon = styled(Icon)`
  && {
    font-size: 24px;
  }
`;

const AndMore = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
  `,
);

const renderIcon = (source: DataSource) =>
  source.type === 'brand' ? <BrandIcon name={source.name} /> : <MaterialIcon name={source.name} />;

// Static, non-interactive list of example data-source icons for the first-use welcome page.
const DataSourceIcons = () => (
  <Sources>
    {DATA_SOURCES.map((source) => (
      <IconCard key={source.label} title={source.label}>{renderIcon(source)}</IconCard>
    ))}
    <AndMore>And more</AndMore>
  </Sources>
);

export default DataSourceIcons;
