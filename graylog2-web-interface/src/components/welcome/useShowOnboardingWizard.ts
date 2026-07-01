import useFeature from 'hooks/useFeature';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';
import usePermissions from 'hooks/usePermissions';
import { REQUIRED_PERMISSIONS } from 'components/welcome/Constants';

type ShowOnboardingWizard = 'LOADING' | 'SHOW' | 'FINISHED' | 'WAIT_FOR_ADMIN';

const useShowOnboardingWizard = (): ShowOnboardingWizard => {
  const onboardingEnabled = useFeature('onboarding_experience');
  const { data, isLoading } = useOnboardingEligibility(onboardingEnabled);
  const firstUse = onboardingEnabled && data?.eligible;
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
