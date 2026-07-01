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

import { Alert } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import useCurrentUser from 'hooks/useCurrentUser';
import { isAnyPermitted } from 'util/PermissionsMixin';
import { REQUIRED_PERMISSIONS } from 'components/welcome/Constants';

import useOnboardingEligibility from './hooks/useOnboardingEligibility';

const OnboardingBanner = () => {
  const productName = useProductName();
  const { permissions } = useCurrentUser();
  const { data } = useOnboardingEligibility();

  if (!isAnyPermitted(permissions, REQUIRED_PERMISSIONS) && data?.status === 'setup') {
    return (
      <Alert bsStyle="info">
        {productName} is not currently receiving any log data - please contact an administrator so they can begin
        setting up ingestion.
      </Alert>
    );
  }

  return null;
};

export default OnboardingBanner;
