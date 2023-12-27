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

import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import ExpandedCustomFieldTypes from 'components/indices/IndexSetFiledTypeProfiles/ExpandedCustomFieldTypes';
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';

const useExpandedSectionsRenderer = () => {
  const { data: { fieldTypes } } = useFieldTypes();

  return useMemo(() => ({
    customFieldMapping: {
      title: 'Rest Custom Field Mappings',
      content: ({ customFieldMappings, id }: IndexSetFieldTypeProfile) => (
        <ExpandedCustomFieldTypes customFieldMappings={customFieldMappings}
                                  fieldTypes={fieldTypes}
                                  profileId={id} />
      ),
    },
  }), [fieldTypes]);
};

export default useExpandedSectionsRenderer;
