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
import { Trans, useTranslation } from 'react-i18next';
import { useState } from 'react';
import styled, { css } from 'styled-components';

import useProductName from 'brand-customization/useProductName';
import useOnboardingCustomization from 'brand-customization/useOnboardingCustomization';
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
import DocsHelper from 'util/DocsHelper';
import PlatformIcons from 'components/collectors/overview/onboarding/PlatformIcons';
import PreviewBadge from 'components/common/PreviewBadge';
import PageHeader from 'components/common/PageHeader';

import useDismissOnboarding from './hooks/useDismissOnboarding';
import DataSourceIcons from './DataSourceIcons';
import IconCard from './IconCard';

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
  display: flex;
`;

const StyledResourceCol = styled(Col)`
  display: flex;
`;

const ResourceTileBody = styled.div`
  display: flex;
  flex-direction: column;
  align-self: stretch;
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
  /** Identifies the resource in telemetry, so it stays English. */
  id: string;
  title: string;
  description: string;
  link: string;
  iconName: IconName;
};

const resources: Resource[] = [
  {
    id: 'Collector Documentation',
    title: 'onboarding.collector_docs',
    description: 'onboarding.collector_docs_description',
    link: DocsHelper.PAGES.COLLECTORS,
    iconName: 'menu_book',
  },
  {
    id: 'Community Forum',
    title: 'onboarding.community_forum',
    description: 'onboarding.community_forum_description',
    link: 'https://community.graylog.org/',
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

const BoxActions = styled.div`
  margin-top: auto;
`;

const SourcesBoxActions = styled(BoxActions)(
  ({ theme }) => css`
    padding-top: ${theme.spacings.lg};
  `,
);

const FirstUseWelcome = () => {
  const { t } = useTranslation('welcome');
  const productName = useProductName();
  const { isResourceSectionEnabledForBrand } = useOnboardingCustomization();
  const { mutate: dismiss } = useDismissOnboarding();
  const [showDismissConfirm, setShowDismissConfirm] = useState(false);
  const sendTelemetry = useSendTelemetry('welcome');

  return (
    <>
      <ConfirmDialog
        show={showDismissConfirm}
        title={t('onboarding.dismiss_title')}
        btnConfirmText={t('onboarding.dismiss_confirm')}
        onConfirm={() => {
          dismiss();
          setShowDismissConfirm(false);
        }}
        onCancel={() => setShowDismissConfirm(false)}>
        <Trans t={t} i18nKey="onboarding.dismiss_text" components={{ strong: <strong /> }} />
      </ConfirmDialog>
      <PageHeader
        title={t('welcome_to', { productName })}
        topActions={
          <DismissButton
            bsStyle="transparent"
            onClick={() => {
              sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.DISMISS_CLICKED, {
                app_action_value: 'dismiss-onboarding-button',
              });
              setShowDismissConfirm(true);
            }}>
            <Icon name="close" /> {t('onboarding.dismiss')}
          </DismissButton>
        }>
        <span>{t('onboarding.sources', { productName })}</span>
      </PageHeader>

      <Row className={'content'}>
        <Col xs={12}>
          <ActionsHeadline>{t('onboarding.where_to_start')}</ActionsHeadline>
          <Section>
            <StyledSectionBox
              title={t('onboarding.enroll_collector')}
              titleAs="h3"
              header={
                <>
                  {t('onboarding.enroll_collector')} <PreviewBadge />
                </>
              }>
              <Description>{t('onboarding.collector_description', { productName })}</Description>
              <PlatformIcons />
              <SourcesBoxActions>
                <LinkContainer to={Routes.SYSTEM.COLLECTORS.OVERVIEW}>
                  <Button
                    bsStyle="primary"
                    onClick={() =>
                      sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.SETUP_COLLECTOR_CLICKED, {
                        app_action_value: 'setup-collector-button',
                      })
                    }>
                    {t('onboarding.enroll_collector')}
                  </Button>
                </LinkContainer>
              </SourcesBoxActions>
            </StyledSectionBox>
            <HideOnCloud>
              <StyledSectionBox title={t('onboarding.set_up_input')} titleAs="h3">
                <Description>{t('onboarding.input_description', { productName })}</Description>
                <DataSourceIcons />
                <BoxActions>
                  <LinkContainer to={Routes.SYSTEM.INPUTS}>
                    <Button
                      bsStyle="default"
                      onClick={() =>
                        sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.CONFIGURE_INPUT_CLICKED, {
                          app_action_value: 'configure-input-button',
                        })
                      }>
                      {t('onboarding.configure_input')}
                    </Button>
                  </LinkContainer>
                </BoxActions>
              </StyledSectionBox>
            </HideOnCloud>
          </Section>
        </Col>
      </Row>

      {isResourceSectionEnabledForBrand && (
        <>
          <SecondaryHeadline>{t('onboarding.resources')}</SecondaryHeadline>

          <Container>
            {resources.map((resource) => (
              <ResourceTile className="content" key={resource.id}>
                <StyledResourceCol>
                  <ResourceTileContent>
                    <IconCard>
                      <Icon name={resource.iconName} />
                    </IconCard>
                    <ResourceTileBody>
                      <ResourceTitle>{t(resource.title)}</ResourceTitle>
                      <ResourceDescription>{t(resource.description, { productName })}</ResourceDescription>
                      <BoxActions>
                        <ExternalLinkButton
                          href={DocsHelper.toString(resource.link)}
                          bsSize="xs"
                          onClick={() =>
                            sendTelemetry(TELEMETRY_EVENT_TYPE.WELCOME.RESOURCE_CONTINUE_CLICKED, {
                              app_action_value: resource.id,
                            })
                          }>
                          {t('onboarding.continue')}
                        </ExternalLinkButton>
                      </BoxActions>
                    </ResourceTileBody>
                  </ResourceTileContent>
                </StyledResourceCol>
              </ResourceTile>
            ))}
          </Container>
        </>
      )}
    </>
  );
};
export default FirstUseWelcome;
