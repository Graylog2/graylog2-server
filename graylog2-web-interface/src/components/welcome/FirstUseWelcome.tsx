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

import useProductName from 'brand-customization/useProductName';
import SectionComponent from 'components/common/Section/SectionComponent';
import LinkContainer from 'components/common/LinkContainer';
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import PlatformIcons from 'components/collectors/overview/onboarding/PlatformIcons';

import useDismissOnboarding from './hooks/useDismissOnboarding';
import DataSourceIcons from './DataSourceIcons';

import PageHeader from '../common/PageHeader';
const StyledSectionComponent = styled(SectionComponent)`
  flex: 1;
`;

const Description = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <p className="description">{children}</p>
);

const resources = [
  {
    title: 'Quickstart Guide',
    description: 'End-to-end walkthrough to your first search in 10 minutes.',
    link: 'https://www.graylog.org',
  },
  {
    title: 'Video: Sidecar',
    description: 'Setup Install and configure your first collector agent.',
    link: 'https://www.graylog.org',
  },
  {
    title: 'Community Forum',
    description: 'Ask questions and browse solutions from other Graylog users.',
    link: 'https://www.graylog.org',
  },
];

const Section = styled.div`
  display: flex;
  column-gap: 40px;
`;

const Resources = styled.h3`
  padding-top: 2rem;
  padding-bottom: 1rem;
`;

const ActionsSection = styled(Section)`
  padding-top: 1rem;
`;

const BoxActions = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: ${theme.spacings.md};
    margin-top: ${theme.spacings.md};
  `,
);

const FirstUseWelcome = () => {
  const productName = useProductName();
  const { mutate: dismiss } = useDismissOnboarding();

  return (
    <>
      <PageHeader title={`Welcome to ${productName}!`}>
        <span>
          {productName} connects to dozens of sources; servers, firewalls, cloud apps, and more.{' '}
          <strong>Where would you like to start?</strong>
        </span>
        <Button bsStyle="link" onClick={() => dismiss()}>
          Dismiss
        </Button>
      </PageHeader>
      <ActionsSection>
        <StyledSectionComponent title="Endpoint Logging">
          <Description>
            Install a lightweight agent on your servers, VMs, or containers. {productName} Sidecar manages the
            configuration automatically.
          </Description>
          <BoxActions>
            <PlatformIcons />
            <LinkContainer to={Routes.SYSTEM.COLLECTORS.OVERVIEW}>
              <Button bsStyle="primary">Set up Collector</Button>
            </LinkContainer>
          </BoxActions>
        </StyledSectionComponent>
        <StyledSectionComponent title="Other Data Sources">
          <Description>
            Open a network listener that accepts logs directly over GELF, Syslog, Beats, or other protocols.
          </Description>
          <BoxActions>
            <DataSourceIcons />
            <LinkContainer to={Routes.SYSTEM.INPUTS}>
              <Button bsStyle="primary">Configure Input</Button>
            </LinkContainer>
          </BoxActions>
        </StyledSectionComponent>
      </ActionsSection>

      <Resources>Resources</Resources>

      <Section>
        {resources.map((resource) => (
          <LinkContainer key={resource.title} to={resource.link}>
            <StyledSectionComponent title={resource.title}>
              <Description>{resource.description}</Description>
            </StyledSectionComponent>
          </LinkContainer>
        ))}
      </Section>
    </>
  );
};
export default FirstUseWelcome;
