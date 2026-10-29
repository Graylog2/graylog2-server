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
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import ExpandedSectionBadge from 'components/common/EntityDataTable/ExpandedSectionBadge';

import { originBsStyles } from './OriginBadge';

type Props = {
  entityId: string;
  origin: FieldTypeOrigin;
};

const OriginCell = ({ entityId, origin }: Props) => {
  const { attributes } = useTableFetchContext();

  const normalizedOrigin = useMemo(() => {
    const originOptions = attributes?.find(({ id }) => id === 'origin')?.filter_options;

    return keyBy(originOptions, 'value');
  }, [attributes]);

  return (
    <ExpandedSectionBadge
      entityId={entityId}
      section="overriddenProfile"
      label="origin explanation"
      bsStyle={originBsStyles[origin]}>
      {normalizedOrigin?.[origin]?.title}
    </ExpandedSectionBadge>
  );
};

export default OriginCell;
