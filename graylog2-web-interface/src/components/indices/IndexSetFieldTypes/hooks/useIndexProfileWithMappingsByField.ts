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
import { useStore } from 'stores/connect';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';
import type { ProfileWithMappingsByField } from 'components/indices/IndexSetFieldTypes/types';

const useIndexProfileWithMappingsByField = () => {
  const { indexSet } = useStore(IndexSetsStore);
  const { data: { customFieldMappings, name, description }, isFetched } = useProfile(indexSet?.field_type_profile);
  const { data: { fieldTypes }, isLoading } = useFieldTypesForMappings();

  const customFieldMappingsByField = useMemo(() => {
    const profileId = indexSet?.field_type_profile;

    if (isFetched && !isLoading && profileId) {
      return mapValues(
        keyBy(customFieldMappings, 'field'), (mapping: CustomFieldMapping) => fieldTypes[mapping.type],
      );
    }

    return {};
  }, [customFieldMappings, fieldTypes, indexSet?.field_type_profile, isFetched, isLoading]);

  return useMemo<ProfileWithMappingsByField>(() => ({
    customFieldMappingsByField,
    name,
    description,
    id: indexSet?.field_type_profile,
  }), [customFieldMappingsByField, description, name, indexSet?.field_type_profile]);
};

export default useIndexProfileWithMappingsByField;
