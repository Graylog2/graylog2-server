import { useQuery } from '@tanstack/react-query';

import { SystemOutputs } from '@graylog/server-api';

const useOutputTypes = () => {
  const { data, isInitialLoading } = useQuery(['outputs', 'types'], () => SystemOutputs.available());

  return { types: data?.types, isLoading: isInitialLoading };
};

export default useOutputTypes;
