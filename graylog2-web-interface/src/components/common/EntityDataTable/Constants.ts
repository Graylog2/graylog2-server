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

export const CELL_PADDING = 5; // px
export const DEFAULT_COL_MIN_WIDTH = 150; // px
export const DEFAULT_COL_WIDTH = 1; // fraction, similar to CSS unit fr.
export const MORE_ACTIONS_TITLE = 'More';
export const MORE_ACTIONS_HOVER_TITLE = 'More actions';

export const BULK_SELECT_COLUMN_WIDTH = 15 + CELL_PADDING * 2; // px
export const BULK_SELECT_COL_ID = 'bulk-select';

export const ACTIONS_COL_ID = 'actions';

export const UTILITY_COLUMNS = new Set([BULK_SELECT_COL_ID, ACTIONS_COL_ID]);

export const ATTRIBUTE_STATUS = {
  show: 'show',
  hide: 'hide',
} as const;
