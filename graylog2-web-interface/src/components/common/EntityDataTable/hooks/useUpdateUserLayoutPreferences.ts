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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TableLayoutPreferences, TableLayoutPreferencesJSON } from 'components/common/EntityDataTable/types';
import UserNotification from 'util/UserNotification';
import useUserLayoutPreferences, {
  tableLayoutQK,
} from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';

const preferencesToJSON = <T>({
  attributes,
  sort,
  perPage,
  customPreferences,
  order,
}: TableLayoutPreferences<T>): TableLayoutPreferencesJSON<T> => ({
  attributes,
  sort: sort ? { order: sort.direction, field: sort.attributeId } : undefined,
  per_page: perPage,
  custom_preferences: customPreferences,
  order,
});

const postUserLayoutPreferences = <T>(entityTableId: string, prefs: TableLayoutPreferences<T>) =>
  fetch('POST', qualifyUrl(`/entitylists/preferences/${entityTableId}`), preferencesToJSON(prefs));

const useUpdateUserLayoutPreferences = <T>(entityTableId: string) => {
  const qc = useQueryClient();

  // still handy to expose current data to callers if needed
  const { data: currentPrefs } = useUserLayoutPreferences<T>(entityTableId);
  const qk = tableLayoutQK(entityTableId);

  const { mutate, isPending } = useMutation({
    mutationFn: (newPreferences: Partial<TableLayoutPreferences<T>>) => {
      // server expects full payload -> merge with *cache* as source of truth
      const cached = (qc.getQueryData(qk) as TableLayoutPreferences<T>) ?? (currentPrefs as TableLayoutPreferences<T>);
      const merged: TableLayoutPreferences<T> = { ...cached, ...newPreferences };

      return postUserLayoutPreferences(entityTableId, merged);
    },

    // ---- OPTIMISTIC UPDATE ----
    onMutate: async (newPreferences: Partial<TableLayoutPreferences<T>>) => {
      await qc.cancelQueries({ queryKey: qk });

      const previous = qc.getQueryData(qk) as TableLayoutPreferences<T> | undefined;
      const next = { ...(previous ?? ({} as TableLayoutPreferences<T>)), ...newPreferences };

      // single source of truth: update the cache immediately
      qc.setQueryData(qk, next);

      // context for rollback
      return { previous };
    },

    onError: (error, _vars, ctx) => {
      if (ctx?.previous) qc.setQueryData(qk, ctx.previous);
      UserNotification.error(`Updating table layout preferences failed with error: ${String(error)}`);
    },

    // Confirm with the server and sync if anything changed
    onSettled: () => {
      qc.invalidateQueries({ queryKey: qk });
    },
  });

  return { mutate, isPending, currentPrefs };
};

export default useUpdateUserLayoutPreferences;
