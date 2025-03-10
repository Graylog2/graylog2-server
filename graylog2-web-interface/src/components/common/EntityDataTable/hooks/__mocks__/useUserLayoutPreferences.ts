import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

import type originalUseUserLayoutPreferences from '../useUserLayoutPreferences';

const useUserLayoutPreferences = jest.fn(
  (): ReturnType<typeof originalUseUserLayoutPreferences> => ({
    data: layoutPreferences,
    isInitialLoading: false,
    refetch: () => {},
  }),
);
export default useUserLayoutPreferences;
