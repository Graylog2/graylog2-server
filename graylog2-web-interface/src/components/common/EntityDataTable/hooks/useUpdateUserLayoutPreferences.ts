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
import type {
  SlicingPreferences,
  SlicingPreferencesJSON,
  TableLayoutPreferences,
  TableLayoutPreferencesJSON,
} from 'components/common/EntityDataTable/types';
import UserNotification from 'util/UserNotification';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';

const slicingToJSON = (slicing?: SlicingPreferences | null): SlicingPreferencesJSON | null | undefined => {
  if (slicing === null) {
    return null;
  }

  if (!slicing) {
    return undefined;
  }

  return {
    slice_column: slicing.sliceColumn,
    sort_by: slicing.sortBy,
    order: slicing.order,
  };
};

const preferencesToJSON = <T>({
  attributes,
  sort,
  perPage,
  slicing,
  customPreferences,
  order,
}: TableLayoutPreferences<T>): TableLayoutPreferencesJSON<T> => ({
  attributes,
  sort: sort ? { order: sort.direction, field: sort.attributeId } : undefined,
  per_page: perPage,
  slicing: slicingToJSON(slicing),
  custom_preferences: customPreferences,
  order,
});

const preferencesUrl = (entityTableId: string, layoutVariant?: string) => {
  const params = layoutVariant ? `?layout_variant=${encodeURIComponent(layoutVariant)}` : '';

  return qualifyUrl(`/entitylists/preferences/${entityTableId}${params}`);
};

const useUpdateUserLayoutPreferences = <T>(entityTableId: string, layoutVariant?: string) => {
  const { data: userLayoutPreferences = {}, refetch } = useUserLayoutPreferences(entityTableId, layoutVariant);
  const mutationFn = (newPreferences: TableLayoutPreferences<T>) =>
    fetch(
      'POST',
      preferencesUrl(entityTableId, layoutVariant),
      preferencesToJSON({ ...userLayoutPreferences, ...newPreferences }),
    );
  const { mutateAsync } = useMutation({
    mutationFn,
    onError: (error) => {
      UserNotification.error(`Updating table layout preferences failed with error: ${error}`);
    },
    onSuccess: () => refetch(),
  });

  return { mutateAsync };
};

export default useUpdateUserLayoutPreferences;
