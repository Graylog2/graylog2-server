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

import TitleCell from 'components/indices/IndexSetTemplates/cells/TitleCell';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import { StatusIcon } from 'components/common';

const customColumnRenderers: ColumnRenderers<IndexSetTemplate> = {
  attributes: {
    title: {
      renderCell: (title: string, template: IndexSetTemplate) => (
        <TitleCell title={title} id={template.id} isDefault={template.default} isEnabled={template.enabled} />
      ),
    },
    built_in: {
      renderCell: (built_in: boolean, _template) => <StatusIcon active={built_in} />,
    },
  },
};

export default customColumnRenderers;
