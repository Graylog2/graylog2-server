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
import { styled } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import useCurrentUser from 'hooks/useCurrentUser';
import { isAnyPermitted } from 'util/PermissionsMixin';
import { REQUIRED_PERMISSIONS } from 'components/welcome/Constants';
import Store from 'logic/local-storage/Store';

import useOnboardingEligibility from './hooks/useOnboardingEligibility';

const ONBOARDING_BANNER_DISMISSED_KEY = 'welcome-onboarding-banner-dismissed';

const StyledAlert = styled(Alert)`
  margin: 0;
`;

const OnboardingBanner = () => {
  const productName = useProductName();
  const { permissions } = useCurrentUser();
  const { data } = useOnboardingEligibility();
  const [onboardingBannerDismissed, setOnboardingBannerDismissed] = useState(
    !!Store.get(ONBOARDING_BANNER_DISMISSED_KEY),
  );

  const onDismiss = () => {
    Store.set(ONBOARDING_BANNER_DISMISSED_KEY, true);
    setOnboardingBannerDismissed(true);
  };

  if (!onboardingBannerDismissed && !isAnyPermitted(permissions, REQUIRED_PERMISSIONS) && data?.status === 'setup') {
    return (
      <Row className="content">
        <Col xs={12}>
          <StyledAlert bsStyle="info" onDismiss={onDismiss}>
            {productName} is not currently receiving any log data - please contact an administrator so they can begin
            setting up ingestion.
          </StyledAlert>
        </Col>
      </Row>
    );
  }

  return null;
};

export default OnboardingBanner;
