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
import useFeature from 'hooks/useFeature';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';
import usePermissions from 'hooks/usePermissions';
import { REQUIRED_PERMISSIONS } from 'components/welcome/Constants';

type ShowOnboardingWizard = 'LOADING' | 'SHOW' | 'FINISHED' | 'WAIT_FOR_ADMIN';

const useShowOnboardingWizard = (): ShowOnboardingWizard => {
  const onboardingEnabled = useFeature('onboarding_experience');
  const { data, isLoading } = useOnboardingEligibility(onboardingEnabled);
  const firstUse = onboardingEnabled && data?.status === 'setup';
  const { isAnyPermitted } = usePermissions();
  const isPermitted = isAnyPermitted(REQUIRED_PERMISSIONS);

  if (isLoading) {
    return 'LOADING';
  }

  if (firstUse && !isPermitted) {
    return 'WAIT_FOR_ADMIN';
  }

  return firstUse ? 'SHOW' : 'FINISHED';
};
export default useShowOnboardingWizard;
