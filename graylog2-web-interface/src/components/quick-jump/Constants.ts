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
export const PAGE_TYPE = 'page' as const;
export const LINK_TYPE = 'link' as const;
export const ACTION_TYPE = 'action' as const;
export const ENTITY_TYPE = 'entity' as const;
export const PAGE_WEIGHT = 0.99;
export const BASE_SCORE = 100;
export const LAST_OPENED_ITEMS_LOOKBACK = 50;
export const FEATURE_FLAG = 'quick_jump' as const;
