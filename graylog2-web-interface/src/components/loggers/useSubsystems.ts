import { useQuery } from '@tanstack/react-query';

import { ClusterSystemLoggers } from '@graylog/server-api';

const useSubsystems = () => useQuery(['loggers', 'subsystems'], ClusterSystemLoggers.subsystems);
export default useSubsystems;
