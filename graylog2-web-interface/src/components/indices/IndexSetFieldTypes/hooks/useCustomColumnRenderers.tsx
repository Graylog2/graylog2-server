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

import type { FieldTypeOrigin } from 'components/indices/IndexSetFieldTypes/types';
import ExpandedRowToggleWrapper from 'components/indices/IndexSetFieldTypes/originBadges/ExpandedRowToggleWrapper';
import ProfileBadge from 'components/indices/IndexSetFieldTypes/originBadges/ProfileBadge';
import OverriddenIndexBadge from 'components/indices/IndexSetFieldTypes/originBadges/OverriddenIndexBadge';
import OverriddenProfileBadge from 'components/indices/IndexSetFieldTypes/originBadges/OverriddenProfileBadge';
import IndexBadge from 'components/indices/IndexSetFieldTypes/originBadges/IndexBadge';
import { Icon } from 'components/common';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import type { Attribute } from 'stores/PaginationTypes';

const useCustomColumnRenderers = (attributes: Array<Attribute>) => {
  const { data: { fieldTypes } } = useFieldTypesForMappings();
  const normalizedOrigin = useMemo(() => {
    const originOptions = attributes?.find(({ id }) => id === 'origin')?.filter_options;

    return keyBy(originOptions, 'value');
  }, [attributes]);

  return useMemo(() => ({
    attributes: {
      type: {
        renderCell: (item: string) => <span>{fieldTypes[item]}</span>,
      },
      origin: {
        renderCell: (origin: FieldTypeOrigin, { id }) => {
          switch (origin) {
            case 'PROFILE':
              return (
                <ExpandedRowToggleWrapper id={id}>
                  <ProfileBadge title={normalizedOrigin?.PROFILE?.title} />
                </ExpandedRowToggleWrapper>
              );
            case 'OVERRIDDEN_INDEX':
              return (
                <ExpandedRowToggleWrapper id={id}>
                  <OverriddenIndexBadge title={normalizedOrigin?.OVERRIDDEN_INDEX?.title} />
                </ExpandedRowToggleWrapper>
              );
            case 'OVERRIDDEN_PROFILE':
              return (
                <ExpandedRowToggleWrapper id={id}>
                  <OverriddenProfileBadge title={normalizedOrigin?.OVERRIDDEN_PROFILE?.title} />
                </ExpandedRowToggleWrapper>
              );
            default:
              return (
                <ExpandedRowToggleWrapper id={id}>
                  <IndexBadge title={normalizedOrigin?.INDEX?.title} />
                </ExpandedRowToggleWrapper>
              );
          }
        },
        staticWidth: 200,
      },
      is_reserved: {
        renderCell: (isReserved: boolean) => (isReserved
          ? <Icon title="Field has reserved field type" name="check" /> : null),
        staticWidth: 120,
      },
    },
  }), [fieldTypes, normalizedOrigin]);
};

export default useCustomColumnRenderers;
