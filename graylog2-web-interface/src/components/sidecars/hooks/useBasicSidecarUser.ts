import { useQuery } from '@tanstack/react-query';

import { Sidecar } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

const BASIC_FORWARDER_USER_QUERY_KEY = 'basic_forwarder_user';

export type BasicSidecarUser = {
  serviceAccount: boolean;
  fullName: string;
  readOnly: boolean;
  id: string;
  username: string;
};

const getSidecarUser = async (): Promise<BasicSidecarUser> =>
  Sidecar.getBasicSidecarUser().then((userResponse) => ({
    serviceAccount: userResponse.service_account,
    fullName: userResponse.full_name,
    readOnly: userResponse.read_only,
    id: userResponse.id,
    username: userResponse.username,
  }));

const useBasicSidecarUser = (): {
  data: BasicSidecarUser;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: [BASIC_FORWARDER_USER_QUERY_KEY],
    queryFn: () =>
      defaultOnError(getSidecarUser(), `Loading Sidecar User failed with status`, `Could not load Sidecar User`),
  });

  return {
    data,
    isLoading,
    refetch,
  };
};

export default useBasicSidecarUser;
