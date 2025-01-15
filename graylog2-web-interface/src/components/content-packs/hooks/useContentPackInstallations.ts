import { useQuery } from '@tanstack/react-query';

import { SystemContentPacks } from '@graylog/server-api';

const useContentPackInstallations = (id: string) => useQuery(
  ['content-packs', 'installations', id],
  () => SystemContentPacks.listContentPackInstallationsById(id),
);
export default useContentPackInstallations;
