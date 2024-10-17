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
import type React from 'react';

type DataTableElementProps = {
  /** Element to be formatted. */
  element?: any;
  /**
   * Formatter function. It expects to receive the `element`, and `index` as arguments and
   * returns an element to be rendered.
   */
  formatter: (...args: any[]) => React.ReactElement;
  /** Element index. */
  index?: number;
};

/**
 * Component used to encapsulate each header or row inside a `DataTable`. You probably
 * should not use this component directly, but through `DataTable`. Look at the `DataTable`
 * section for a usage example.
 */
const DataTableElement = ({ formatter, element, index }: DataTableElementProps) => formatter(element, index);

export default DataTableElement;
