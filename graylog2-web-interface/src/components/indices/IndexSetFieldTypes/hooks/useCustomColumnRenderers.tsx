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

import type { FieldTypeOrigin } from 'components/indices/IndexSetFieldTypes/types';
import ExpandedRowToggleWrapper from 'components/indices/IndexSetFieldTypes/originBadges/ExpandedRowToggleWrapper';
import { Icon } from 'components/common';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import OriginCell from 'components/indices/IndexSetFieldTypes/originBadges/OriginCell';

const useCustomColumnRenderers = () => {
  const { data: { fieldTypes } } = useFieldTypesForMappings();

  return useMemo(() => ({
    attributes: {
      type: {
        renderCell: (item: string) => <span>{fieldTypes[item]}</span>,
      },
      origin: {
        renderCell: (origin: FieldTypeOrigin, { id }) => (
          <ExpandedRowToggleWrapper id={id}>
            <OriginCell origin={origin} />
          </ExpandedRowToggleWrapper>
        ),
        staticWidth: 200,
      },
      is_reserved: {
        renderCell: (isReserved: boolean) => (isReserved
          ? <Icon title="Field has reserved field type" name="check" /> : null),
        staticWidth: 120,
      },
    },
  }), [fieldTypes]);
};

export default useCustomColumnRenderers;
