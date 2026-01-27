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
import { RelativeTime } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { Fleet } from '../types';

const customColumnRenderers = (): ColumnRenderers<Fleet> => ({
  attributes: {
    name: {
      renderCell: (_name: string, fleet: Fleet) => (
        <Link to={Routes.SYSTEM.COLLECTORS.FLEET(fleet.id)}>
          {fleet.name}
        </Link>
      ),
      width: 0.25,
    },
    description: {
      renderCell: (description: string) => (
        <span>{description || '—'}</span>
      ),
      width: 0.4,
    },
    target_version: {
      renderCell: (version: string | null) =>
        version ? <Badge bsStyle="info">v{version}</Badge> : <span>—</span>,
      staticWidth: 120,
    },
    created_at: {
      renderCell: (_createdAt: string, fleet: Fleet) => (
        <RelativeTime dateTime={fleet.created_at} />
      ),
      width: 0.15,
    },
  },
});

export default customColumnRenderers;
