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
import styled from 'styled-components';
import type { Permission } from 'graylog-web-plugin/plugin';

import { Alert, Button } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import useCurrentUser from 'hooks/useCurrentUser';
import { isAnyPermitted } from 'util/PermissionsMixin';

import IngestionSetupModal from './IngestionSetupModal';
import useOnboardingEligibility from './hooks/useOnboardingEligibility';
import useDismissOnboarding from './hooks/useDismissOnboarding';

// `collectors:create` is a valid backend permission that is not (yet) part of the generated
// `Permission` union, hence the assertion.
const REQUIRED_PERMISSIONS = ['inputs:create', 'collectors:create'] as Array<Permission>;

// Inline link button that matches the surrounding sentence instead of using a button-sized font.
const InlineLink = styled(Button)`
  font-size: inherit;
  vertical-align: baseline;
`;

const OnboardingBanner = () => {
  const productName = useProductName();
  const { permissions } = useCurrentUser();
  const { data, isLoading } = useOnboardingEligibility();
  const { mutate: dismiss } = useDismissOnboarding();
  const [showSetupModal, setShowSetupModal] = useState(false);

  if (isLoading || !data?.eligible) {
    return null;
  }

  if (!isAnyPermitted(permissions, REQUIRED_PERMISSIONS)) {
    return (
      <Alert bsStyle="info">
        {productName} is not currently receiving any log data — please contact an administrator so they can begin
        setting up ingestion.
      </Alert>
    );
  }

  return (
    <>
      <Alert bsStyle="info" onDismiss={dismiss}>
        {productName} is not currently receiving any log data. Click{' '}
        <InlineLink bsStyle="link" onClick={() => setShowSetupModal(true)}>
          here
        </InlineLink>{' '}
        to set up ingestion.
      </Alert>
      <IngestionSetupModal show={showSetupModal} onHide={() => setShowSetupModal(false)} />
    </>
  );
};

export default OnboardingBanner;
