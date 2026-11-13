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
import type * as React from 'react';
import { useState } from 'react';

export const DEFAULT_COLLAPSED_COUNT = 1;

/**
 * State of an expandable list in an entity data table cell: whether it is expanded and how many items are hidden.
 */
const useCellListToggle = (itemCount: number, collapsedCount: number) => {
  const [isExpanded, setIsExpanded] = useState(false);

  // The list is part of a table row, so the toggle must not act on the row as well.
  const toggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded((cur) => !cur);
  };

  return { isExpanded, hiddenCount: itemCount - collapsedCount, toggle };
};

export default useCellListToggle;
