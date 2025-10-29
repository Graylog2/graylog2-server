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
`;

const TableWrapper = styled.div`
  margin-top: ${({ theme }) => theme.spacings.xs};
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
    <Section title={title} collapsible={collapsible} headerLeftSection={headerLeftSection}>
      <TableWrapper>{children}</TableWrapper>
    </Section>
  </Container>
);

export default ClusterNodesSectionWrapper;
