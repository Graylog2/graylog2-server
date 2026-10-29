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

import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import { OverlayTrigger } from 'components/common';
import OriginExpandedSection from 'components/indices/IndexSetFieldTypes/originExpandedSections/OriginExpandedSection';

import OriginBadge from './OriginBadge';

type Props = {
  fieldType: IndexSetFieldType;
};

const OriginCell = ({ fieldType: { origin, type, fieldName } }: Props) => {
  const { attributes } = useTableFetchContext();

  const normalizedOrigin = useMemo(() => {
    const originOptions = attributes?.find(({ id }) => id === 'origin')?.filter_options;

    return keyBy(originOptions, 'value');
  }, [attributes]);

  return (
    <OverlayTrigger
      trigger="hover"
      placement="bottom"
      overlay={<OriginExpandedSection origin={origin} type={type} fieldName={fieldName} />}>
      <OriginBadge origin={origin} title={normalizedOrigin?.[origin]?.title} withInfoIcon />
    </OverlayTrigger>
  );
};

export default OriginCell;
