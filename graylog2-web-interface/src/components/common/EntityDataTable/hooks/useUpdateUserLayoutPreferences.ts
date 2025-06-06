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
import { useMutation } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TableLayoutPreferences, TableLayoutPreferencesJSON } from 'components/common/EntityDataTable/types';
import UserNotification from 'util/UserNotification';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';

const preferencesToJSON = <T>({
  displayedAttributes,
  sort,
  perPage,
  customPreferences,
}: TableLayoutPreferences<T>): TableLayoutPreferencesJSON<T> => ({
  displayed_attributes: displayedAttributes,
  sort: sort ? { order: sort.direction, field: sort.attributeId } : undefined,
  per_page: perPage,
  custom_preferences: customPreferences,
});

const useUpdateUserLayoutPreferences = <T>(entityTableId: string) => {
  const { data: userLayoutPreferences = {}, refetch } = useUserLayoutPreferences(entityTableId);
  const mutationFn = (newPreferences: TableLayoutPreferences<T>) =>
    fetch(
      'POST',
      qualifyUrl(`/entitylists/preferences/${entityTableId}`),
      preferencesToJSON({ ...userLayoutPreferences, ...newPreferences }),
    );
  const { mutate } = useMutation({
    mutationFn,
    onError: (error) => {
      UserNotification.error(`Updating table layout preferences failed with error: ${error}`);
    },
    onSuccess: () => refetch(),
  });

  return { mutate };
};

export default useUpdateUserLayoutPreferences;
