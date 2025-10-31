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
import React from 'react';
import styled from 'styled-components';

import { Section } from 'components/common';

const Container = styled.div`
  > div {
    border: none;
  }

  table thead,
  table thead tr,
  table thead th {
    background-color: transparent !important;
  }

  table tbody {
    background-color: transparent;
  }

  table tbody tr {
    background-color: transparent;
  }

  table tbody tr:first-of-type {
    background-color: transparent;
  }

  table tbody:nth-of-type(odd) tr:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.background} !important;
  }

  table tbody:nth-of-type(even) tr:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.backgroundStriped} !important;
  }

  table tbody tr:hover:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.backgroundHover} !important;
  }
`;

const TableWrapper = styled.div`
  margin-top: calc(-1 * ${({ theme }) => theme.spacings.lg});
`;

type Props = React.PropsWithChildren<{
  title: string;
  headerLeftSection?: React.ReactNode;
  collapsible?: boolean;
}>;

const ClusterNodesSectionWrapper = ({
  children = null,
  title,
  headerLeftSection = undefined,
  collapsible = true,
}: Props) => (
  <Container>
    <Section
      title={title}
      collapsible={collapsible}
      headerLeftSection={headerLeftSection}
      collapseButtonPosition="left">
      <TableWrapper>{children}</TableWrapper>
    </Section>
  </Container>
);

export default ClusterNodesSectionWrapper;
