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
import React, { useMemo } from 'react';
import keyBy from 'lodash/keyBy';
import mapValues from 'lodash/mapValues';

import {
  PaginatedEntityTable,
} from 'components/common';
import type { Sort } from 'stores/PaginationTypes';
import type {
  IndexSetFieldTypeProfile,
} from 'components/indices/IndexSetFieldTypeProfiles/types';
import { fetchIndexSetFieldTypeProfiles, keyFn } from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfiles';
import useCustomColumnRenderers from 'components/indices/IndexSetFieldTypeProfiles/helpers/useCustomColumnRenderers';
import profileActions from 'components/indices/IndexSetFieldTypeProfiles/helpers/profileActions';
import { useStore } from 'stores/connect';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';
import ExpandedCustomFieldTypes from 'components/indices/IndexSetFieldTypeProfiles/ExpandedCustomFieldTypes';

export const DEFAULT_LAYOUT = {
  entityTableId: 'index-set-field-type-profiles',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'name', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['name', 'description', 'custom_field_mappings', 'index_set_ids'],
};

const expandedSections = {
  customFieldMapping: {
    title: 'Custom Field Mappings',
    content: ({ customFieldMappings }: IndexSetFieldTypeProfile) => (
      <ExpandedCustomFieldTypes customFieldMappings={customFieldMappings} />
    ),
  },
};

const COLUMNS_ORDER = ['name', 'description', 'custom_field_mappings', 'index_set_ids'];

const ProfilesList = () => {
  const { indexSets } = useStore(IndexSetsStore);
  const normalizedIndexSetsTitles = useMemo(() => mapValues(keyBy(indexSets, 'id'), 'title'), [indexSets]);
  const customColumnRenderers = useCustomColumnRenderers(normalizedIndexSetsTitles);

  return (
    <PaginatedEntityTable<IndexSetFieldTypeProfile> humanName="index set profiles"
                                                    columnsOrder={COLUMNS_ORDER}
                                                    entityActions={profileActions}
                                                    tableLayout={DEFAULT_LAYOUT}
                                                    fetchEntities={fetchIndexSetFieldTypeProfiles}
                                                    keyFn={keyFn}
                                                    entityAttributesAreCamelCase
                                                    expandedSectionsRenderer={expandedSections}
                                                    columnRenderers={customColumnRenderers}
                                                    searchPlaceholder="Search for profile name" />
  );
};

export default ProfilesList;
