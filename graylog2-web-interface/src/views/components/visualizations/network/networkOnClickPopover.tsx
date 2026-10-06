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
import scatterOnClickPopover from 'views/components/visualizations/scatter/scatterOnClickPopover';
import sankeyOnClickPopover from 'views/components/visualizations/sankey/sankeyOnClickPopover';

/**
 * The network graph is rendered as scatter traces (nodes as markers, edges as line segments),
 * so it reuses the scatter anchor logic to locate the clicked element. Its popover, however, is
 * shared with sankey: `SankeyOnClickPopover` renders both node and edge interactions in a single
 * dropdown and reads edge metadata from the point's `customdata`.
 */
const networkOnClickPopover = {
  buildAnchor: scatterOnClickPopover.buildAnchor,
  renderPopover: sankeyOnClickPopover.renderPopover,
};

export default networkOnClickPopover;
