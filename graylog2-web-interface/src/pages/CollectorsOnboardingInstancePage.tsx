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
import React, { useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';

import { Row, Col, Alert } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Link } from 'components/common';
import PreviewBadge from 'components/common/PreviewBadge';
import DocsHelper from 'util/DocsHelper';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import ConnectionSuccess from 'components/collectors/overview/onboarding/ConnectionSuccess';
import Routes from 'routing/Routes';
import useLocation from 'routing/useLocation';
import { extractErrorMessage } from 'util/extractErrorMessage';
import useFinishOnboarding from 'components/welcome/hooks/useFinishOnboarding';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';

const CollectorsOnboardingInstancePage = () => {
  const { t } = useTranslation('collectors');
  const { instanceUid } = useParams<{ instanceUid: string }>();
  const location = useLocation<{ fleetName?: string } | null>();
  // Set by the onboarding wizard's history push; absent on direct visits.
  const stateFleetName = location.state?.fleetName;

  const { data: instance, isLoading, error } = useInstance(instanceUid);
  const { data: fleet } = useFleet(instance?.fleet_id ?? '');

  const { mutate: finish } = useFinishOnboarding();

  // Guards that the instance is actually there before we finish the onboarding. `useInstance`
  // polls on the heartbeat interval and returns a new object each time, so this must be latched
  // rather than keyed on the instance reference -- otherwise it re-POSTs every poll.
  // The COMPLETED telemetry event lives in `ConnectionSuccess`, which knows whether messages
  // are actually arriving.
  const finishedFor = useRef<string | null>(null);

  const { data: onboarding } = useOnboardingEligibility();
  const onboardingInProgress = onboarding?.status === 'setup';

  useEffect(() => {
    if (!onboardingInProgress || !instance || finishedFor.current === instance.instance_uid) return;

    finishedFor.current = instance.instance_uid;
    finish();
  }, [onboardingInProgress, instance, finish]);

  const content = () => {
    if (isLoading) return <Spinner />;

    if (error) {
      return <Alert bsStyle="danger">{t('pages.onboarding.load_error', { error: extractErrorMessage(error) })}</Alert>;
    }

    if (!instance) {
      return (
        <Alert bsStyle="warning">
          <Trans
            t={t}
            i18nKey="pages.onboarding.not_found"
            components={{ instancesLink: <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES} /> }}
          />
        </Alert>
      );
    }

    return <ConnectionSuccess instance={instance} fleetName={fleet?.name ?? stateFleetName} />;
  };

  return (
    <DocumentTitle title={t('pages.onboarding.title')}>
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            {t('pages.onboarding.title')} <PreviewBadge />
          </>
        }
        documentationLink={{
          title: t('pages.documentation_title'),
          path: DocsHelper.PAGES.COLLECTORS,
        }}>
        <span>{t('pages.onboarding.description')}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>{content()}</Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsOnboardingInstancePage;
