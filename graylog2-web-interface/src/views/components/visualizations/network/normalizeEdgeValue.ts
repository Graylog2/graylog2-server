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
/**
 * Linearly map an edge's aggregated metric value to a position in [0, 1], used to sample a color
 * from the configured colorscale. When every edge shares the same value (including a single-edge
 * graph) there is no meaningful range, so all edges sample the low end of the scale.
 */
const normalizeEdgeValue = (value: number, min: number, max: number): number =>
  max === min ? 0 : (value - min) / (max - min);

export default normalizeEdgeValue;
