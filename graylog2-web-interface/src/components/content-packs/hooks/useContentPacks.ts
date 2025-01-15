import { useQuery } from '@tanstack/react-query';

import { SystemContentPacks } from '@graylog/server-api';

const useContentPacks = () => useQuery(['content-packs', 'list'], () => SystemContentPacks.listContentPacks());
export default useContentPacks;
