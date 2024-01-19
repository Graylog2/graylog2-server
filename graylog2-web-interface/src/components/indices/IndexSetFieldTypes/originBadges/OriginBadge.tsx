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
import React from 'react';

import { Badge } from 'components/bootstrap';
import type { FieldTypeOrigin } from 'components/indices/IndexSetFieldTypes/types';

type Props = {
  origin: FieldTypeOrigin,
  title: string,
}

type BadgeBsStyle = 'default' | 'danger' | 'info' | 'primary' | 'success' | 'warning' | 'gray'
const originBsStyles: Record<FieldTypeOrigin, BadgeBsStyle> = {
  INDEX: 'gray',
  OVERRIDDEN_INDEX: 'primary',
  OVERRIDDEN_PROFILE: 'warning',
  PROFILE: 'default',
};
const OriginBadge = ({ title, origin }: Props) => (
  <Badge bsStyle={originBsStyles[origin]}>{title}</Badge>
);

export default OriginBadge;
