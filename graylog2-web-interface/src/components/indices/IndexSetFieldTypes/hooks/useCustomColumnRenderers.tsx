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
import { useTranslation } from 'react-i18next';

import type { FieldTypeOrigin } from 'components/indices/IndexSetFieldTypes/types';
import { Icon } from 'components/common';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import OriginCell from 'components/indices/IndexSetFieldTypes/originBadges/OriginCell';
import { ExpandedSectionToggleWrapper } from 'components/common/EntityDataTable';
import { COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';

const useCustomColumnRenderers = () => {
  const { t } = useTranslation('indexsets');
  const {
    data: { fieldTypes },
  } = useFieldTypesForMappings();

  return useMemo(
    () => ({
      attributes: {
        field_name: {
          staticWidth: COLUMN_WIDTH.title,
        },
        type: {
          renderCell: (item: string) => <span>{fieldTypes[item]}</span>,
        },
        origin: {
          renderCell: (origin: FieldTypeOrigin, { id }) => (
            <ExpandedSectionToggleWrapper id={id} section="overriddenProfile">
              <OriginCell origin={origin} />
            </ExpandedSectionToggleWrapper>
          ),
          staticWidth: 150,
        },
        is_reserved: {
          renderCell: (isReserved: boolean) =>
            isReserved ? <Icon title={t('field_types.reserved_icon')} name="check" /> : null,
          staticWidth: 'matchHeader' as const,
        },
      },
    }),
    [fieldTypes, t],
  );
};

export default useCustomColumnRenderers;
