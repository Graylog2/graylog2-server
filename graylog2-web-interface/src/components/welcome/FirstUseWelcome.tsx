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

import useProductName from 'brand-customization/useProductName';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import LinkContainer from 'components/common/LinkContainer';
import ConfirmDialog from 'components/common/ConfirmDialog';
import { Icon, Section as SectionBox, ExternalLinkButton } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { SectionCol } from 'components/common/Section/SectionComponent';
import type { IconName } from 'components/common/Icon/types';
import { Button, Row, Col } from 'components/bootstrap';
import Routes from 'routing/Routes';
import PlatformIcons from 'components/collectors/overview/onboarding/PlatformIcons';

import useDismissOnboarding from './hooks/useDismissOnboarding';
import DataSourceIcons from './DataSourceIcons';
import IconCard from './IconCard';

import PageHeader from '../common/PageHeader';

const Container = styled.div(
  ({ theme }) => css`
    gap: ${theme.spacings.sm};
    display: flex;

    margin-left: -15px;
    margin-right: -15px;
  `,
);

const ResourceTile = styled.div`
  margin: 0;
  flex: 1;
`;

const DismissButton = styled(Button)`
  margin-top: -8px;
  margin-right: -8px;
`;

const ActionsHeadline = styled.h2(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const Description = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <p className="description">{children}</p>
);

type Resource = {
  title: string;
  description: string;
  link: string;
  iconName: IconName;
};

const resources: Resource[] = [
  {
    title: 'Quickstart Guide',
    description: 'End-to-end walkthrough to your first search in 10 minutes.',
    link: 'https://www.graylog.org',
    iconName: 'acute',
  },
  {
    title: 'Video: Sidecar',
    description: 'Setup Install and configure your first collector agent.',
    link: 'https://www.graylog.org',
    iconName: 'arrow_or_edge',
  },
  {
    title: 'Community Forum',
    description: 'Ask questions and browse solutions from other Graylog users.',
    link: 'https://www.graylog.org',
    iconName: 'chat',
  },
];

const Section = styled.div(
  ({ theme }) => css`
    display: flex;
    column-gap: ${theme.spacings.sm};
  `,
);

const StyledSectionBox = styled(SectionBox)`
  flex: 1;
  display: flex;
  flex-direction: column;

  ${SectionCol} {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
`;

const SecondaryHeadline = styled.h2(
  ({ theme }) => css`
    padding-top: ${theme.spacings.lg};
    padding-bottom: ${theme.spacings.md};
  `,
);

const ResourceTitle = styled.h3`
  margin-top: 0;
`;

const ResourceDescription = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    margin-top: ${theme.spacings.xxs};
  `,
);

const ResourceTileContent = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: flex-start;
    gap: ${theme.spacings.md};
    padding: 0 ${theme.spacings.md};
  `,
);

const ActionsSection = styled(Section)``;

const BoxActions = styled.div(
  ({ theme }) => css`
    margin-top: auto;
    padding-top: ${theme.spacings.lg};
  `,
);

const FirstUseWelcome = () => {
  const productName = useProductName();
  const { mutate: dismiss } = useDismissOnboarding();
  const [showDismissConfirm, setShowDismissConfirm] = useState(false);
  const sendTelemetry = useSendTelemetry();

  return (
    <>
      <ConfirmDialog
        show={showDismissConfirm}
        title="Dismiss onboarding for everyone?"
        btnConfirmText="Dismiss for everyone"
        onConfirm={() => {
          dismiss();
          setShowDismissConfirm(false);
        }}
        onCancel={() => setShowDismissConfirm(false)}>
        This turns off the guided onboarding and shows the default welcome page instead for <strong>every user</strong>{' '}
        of this installation.
      </ConfirmDialog>
      <PageHeader
        title={`Welcome to ${productName}!`}
        topActions={
          <DismissButton
            bsStyle="transparent"
            onClick={() => {
              sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.DISMISS_CLICKED, {
                app_section: 'welcome',
                app_action_value: 'dismiss-onboarding-button',
              });
              setShowDismissConfirm(true);
            }}>
            <Icon name="close" /> Dismiss onboarding
          </DismissButton>
        }>
        <span>{productName} connects to dozens of sources; servers, firewalls, cloud apps, and more. </span>
      </PageHeader>

      <Row className={'content'}>
        <Col xs={12}>
          <ActionsHeadline>Where would you like to start?</ActionsHeadline>
          <ActionsSection>
            <StyledSectionBox title="Set up Collectors" titleAs="h3">
              <Description>
                Install a lightweight agent on your servers, VMs, or containers. {productName} Sidecar manages the
                configuration automatically.
              </Description>
              <PlatformIcons />
              <BoxActions>
                <LinkContainer to={Routes.SYSTEM.COLLECTORS.OVERVIEW}>
                  <Button
                    bsStyle="primary"
                    onClick={() =>
                      sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.SETUP_COLLECTOR_CLICKED, {
                        app_section: 'welcome',
                        app_action_value: 'setup-collector-button',
                      })
                    }>
                    Set up Collector
                  </Button>
                </LinkContainer>
              </BoxActions>
            </StyledSectionBox>
            <HideOnCloud>
              <StyledSectionBox title="Set up Other Sources" titleAs="h3">
                <Description>
                  Open a network listener that accepts logs directly over GELF, Syslog, Beats, or other protocols.
                </Description>
                <DataSourceIcons />
                <BoxActions>
                  <LinkContainer to={Routes.SYSTEM.INPUTS}>
                    <Button
                      bsStyle="default"
                      onClick={() =>
                        sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.CONFIGURE_INPUT_CLICKED, {
                          app_section: 'welcome',
                          app_action_value: 'configure-input-button',
                        })
                      }>
                      Configure Input
                    </Button>
                  </LinkContainer>
                </BoxActions>
              </StyledSectionBox>
            </HideOnCloud>
          </ActionsSection>
        </Col>
      </Row>

      <SecondaryHeadline>Resources</SecondaryHeadline>

      <Container>
        {resources.map((resource) => (
          <ResourceTile className="content" key={resource.title}>
            <Col>
              <ResourceTileContent>
                <IconCard>
                  <Icon name={resource.iconName} />
                </IconCard>
                <div>
                  <ResourceTitle>{resource.title}</ResourceTitle>
                  <ResourceDescription>{resource.description}</ResourceDescription>
                  <ExternalLinkButton
                    href={resource.link}
                    bsSize="xs"
                    onClick={() =>
                      sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.RESOURCE_CONTINUE_CLICKED, {
                        app_section: 'welcome',
                        app_action_value: resource.title,
                      })
                    }>
                    Continue
                  </ExternalLinkButton>
                </div>
              </ResourceTileContent>
            </Col>
          </ResourceTile>
        ))}
      </Container>
    </>
  );
};
export default FirstUseWelcome;
