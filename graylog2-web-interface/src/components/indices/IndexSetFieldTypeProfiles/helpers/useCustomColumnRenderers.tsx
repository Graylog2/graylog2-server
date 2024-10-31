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

import type { CustomFieldMapping, IndexSetFieldTypeProfile } from 'components/indices/IndexSetFieldTypeProfiles/types';
import CustomFieldMappingsCell from 'components/indices/IndexSetFieldTypeProfiles/cells/CustomFieldMappingsCell';
import IndexSetsCell from 'components/indices/IndexSetFieldTypeProfiles/cells/IndexSetsCell';

const useCustomColumnRenderers = (normalizedIndexSetsTitles: Record<string, string>) => useMemo(() => ({
  attributes: {
    custom_field_mappings: {
      renderCell: (customFieldTypes: Array<CustomFieldMapping>, profile: IndexSetFieldTypeProfile) => (
        <CustomFieldMappingsCell profile={profile}
                                 customFieldTypes={customFieldTypes} />
      ),
      staticWidth: 200,
    },
    index_set_ids: {
      renderCell: (indexSetIds: Array<string>) => <IndexSetsCell indexSetIds={indexSetIds} normalizedIndexSetsTitles={normalizedIndexSetsTitles} />,
    },
  },
}), [normalizedIndexSetsTitles]);

export default useCustomColumnRenderers;
