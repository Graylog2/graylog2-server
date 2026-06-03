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
import * as React from 'react';

import { Badge } from 'components/bootstrap';
import type { NotificationType } from 'components/notifications/types';

const COLOR_BY_SEVERITY: Record<string, 'danger' | 'info' | 'default'> = {
  urgent: 'danger',
  normal: 'info',
};

type Props = { row: NotificationType };

const capitalize = (value: string) => value.charAt(0).toUpperCase() + value.slice(1);

const SeverityCell = ({ row }: Props) => (
  <Badge bsStyle={COLOR_BY_SEVERITY[row.severity] ?? 'default'}>{capitalize(row.severity)}</Badge>
);

export default SeverityCell;
