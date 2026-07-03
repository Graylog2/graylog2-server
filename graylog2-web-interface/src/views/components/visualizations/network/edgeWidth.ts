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
const MIN_EDGE_WIDTH = 1;
const MAX_EDGE_WIDTH = 8;

/**
 * Linearly map an edge's aggregated metric value to a line width in
 * [MIN_EDGE_WIDTH, MAX_EDGE_WIDTH]. When every edge shares the same value
 * (including a single-edge graph) there is no meaningful range, so all edges
 * render at the thinnest width.
 */
const edgeWidth = (value: number, min: number, max: number): number => {
  if (max === min) return MIN_EDGE_WIDTH;

  const t = (value - min) / (max - min);

  return MIN_EDGE_WIDTH + t * (MAX_EDGE_WIDTH - MIN_EDGE_WIDTH);
};

export default edgeWidth;
