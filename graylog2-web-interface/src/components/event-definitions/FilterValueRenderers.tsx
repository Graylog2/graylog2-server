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

import { EventDefinitionTypeRenderer } from 'components/events/events/ColumnRenderers';

// Render the active `type` filter chip with the plugin's display name (e.g. "Filter & Aggregation")
// instead of the raw config.type value (e.g. "aggregation-v1"), matching the Type column.
const FilterValueRenderers = {
  type: (value: string) => <EventDefinitionTypeRenderer type={value} />,
};

export default FilterValueRenderers;
