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
import type { DefaultTheme } from 'styled-components';
import { Box } from '@mantine/core';
import type { BoxProps, TitleOrder } from '@mantine/core';

import Col from 'preflight/components/common/Col';
import Row from 'preflight/components/common/Row';
import { Title } from 'preflight/components/common';

type ContainerType = BoxProps & {
  theme: DefaultTheme,
  component: any;
};

const TitleActionContainer = styled(Box)`
  display: flex;
  justify-content: flex-end;
  gap: 5px;
`;
const SubsectionContainer = styled(Box)<React.PropsWithChildren<ContainerType>>(({ theme }: ContainerType) => css`
  padding: ${theme.spacings.md};
  margin-bottom: ${theme.spacings.xs};
`);

const SectionContainer = styled(SubsectionContainer)(({ theme }: ContainerType) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.variant.lighter.default};
  border-radius: 4px;
`);

type Props = {
  title: React.ReactNode,
  actions?: React.ReactNode,
  titleOrder?: TitleOrder
  dataTestid?: string,
};

const SectionHeader = ({ title, actions, titleOrder = 2 }: Props) => (
  <Row>
    <Col span={{ base: 12, lg: 6, md: 6 }}>
      <Title order={titleOrder}>{title}</Title>
    </Col>
    <Col span={{ base: 12, lg: 6, md: 6 }}>
      <TitleActionContainer>{actions}</TitleActionContainer>
    </Col>
  </Row>
);

export const Subsection = ({ title, children, actions, titleOrder }: React.PropsWithChildren<Props>) => (
  <SubsectionContainer component="section">
    <SectionHeader title={title} actions={actions} titleOrder={titleOrder} />
    {children}
  </SubsectionContainer>
);

const Section = ({ title, children, actions, titleOrder, dataTestid }: React.PropsWithChildren<Props>) => (
  <SectionContainer component="section" data-testid={dataTestid}>
    <SectionHeader title={title} actions={actions} titleOrder={titleOrder} />
    {children}
  </SectionContainer>
);

export default Section;
