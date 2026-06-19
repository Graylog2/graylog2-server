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

import type { OnClickPopoverDropdown } from 'views/components/visualizations/OnClickPopover/Types';
import type { RenderPopover } from 'views/components/visualizations/OnClickPopover/anchors';
import DropdownSwitcher from 'views/components/visualizations/OnClickPopover/DropdownSwitcher';

/**
 * Builds a `renderPopover` that drives the shared `DropdownSwitcher` with the given
 * dropdown component. Used by the chart types whose popover only differs by which
 * "values" dropdown they render (bar, scatter, pie, heatmap).
 */
const dropdownPopover =
  (Component: OnClickPopoverDropdown): RenderPopover =>
  ({ anchor, config, onPopoverClose }) => (
    <DropdownSwitcher
      component={Component}
      clickPoint={anchor.pt}
      config={config}
      clickPointsInRadius={anchor.pointsInRadius}
      onPopoverClose={onPopoverClose}
    />
  );

export default dropdownPopover;
