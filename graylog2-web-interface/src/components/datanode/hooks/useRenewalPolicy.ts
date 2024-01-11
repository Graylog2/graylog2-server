import { useQuery } from '@tanstack/react-query';

import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import ConfigurationType from 'components/configurations/ConfigurationTypes';
import type { RenewalPolicy } from 'components/datanode/Types';
import type FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';

const queryKey = ['config', 'certificate-renewal-policy'];
const fetchCurrentConfig = () => ConfigurationsActions.list(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG) as Promise<RenewalPolicy>;

const UseRenewalPolicy = (): {
  data: RenewalPolicy,
  isInitialLoading: boolean,
  error: FetchError | null,
} => {
  const { data, isInitialLoading, error } = useQuery<RenewalPolicy, FetchError>(queryKey, fetchCurrentConfig, {
    initialData: undefined,
    refetchInterval: 3000,
    retry: false,
    onError: (newError) => {
      UserNotification.error(`Loading Data Nodes failed with status: ${newError}`,
        'Could not load Data Nodes');
    },
  });

  return {
    data,
    isInitialLoading,
    error,
  };
};

export default UseRenewalPolicy;
