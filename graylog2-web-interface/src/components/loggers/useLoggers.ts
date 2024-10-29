import { useQuery } from '@tanstack/react-query';

import { ClusterSystemLoggers } from '@graylog/server-api';

const useLoggers = () => useQuery(['loggers', 'loggers'], ClusterSystemLoggers.loggers);
export default useLoggers;
