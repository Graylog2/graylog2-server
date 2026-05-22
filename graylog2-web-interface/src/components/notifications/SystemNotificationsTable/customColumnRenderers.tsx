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

import type { NotificationType } from 'components/notifications/types';

import TitleCell from './cells/TitleCell';
import DescriptionCell from './cells/DescriptionCell';
import TriggeredAtCell from './cells/TriggeredAtCell';
import SeverityCell from './cells/SeverityCell';

const customColumnRenderers = {
  attributes: {
    title: {
      renderCell: (_title: string, row: NotificationType) => <TitleCell row={row} />,
    },
    severity: {
      renderCell: (_severity: string, row: NotificationType) => <SeverityCell row={row} />,
    },
    description: {
      renderCell: (_description: string, row: NotificationType) => <DescriptionCell row={row} />,
    },
    timestamp: {
      renderCell: (triggeredAt: string) => <TriggeredAtCell triggeredAt={triggeredAt} />,
    },
  },
};

export default customColumnRenderers;
