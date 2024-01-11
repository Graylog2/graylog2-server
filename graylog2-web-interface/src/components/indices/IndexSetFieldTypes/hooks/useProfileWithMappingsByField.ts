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
import { useMemo } from 'react';
import keyBy from 'lodash/keyBy';
import mapValues from 'lodash/mapValues';

import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';

export type ProfileWithMappingsByField = {
  name: string,
  description?: string,
  id: string,
  customFieldMappingsByField: Record<string, string>
}

const useProfileWithMappingsByField = (profileId: string) => {
  const { data: { customFieldMappings, name, description }, isFetched } = useProfile(profileId);
  const { data: { fieldTypes }, isLoading } = useFieldTypesForMappings();

  const customFieldMappingsByField = useMemo(() => {
    if (isFetched && !isLoading) {
      return mapValues(
        keyBy(customFieldMappings, 'field'), (mapping: CustomFieldMapping) => fieldTypes[mapping.type],
      );
    }

    return {};
  }, [customFieldMappings, fieldTypes, isFetched, isLoading]);

  return useMemo<ProfileWithMappingsByField>(() => ({
    customFieldMappingsByField,
    name,
    description,
    id: profileId,
  }), [customFieldMappingsByField, description, name, profileId]);
};

export default useProfileWithMappingsByField;
