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
import { forwardRef } from 'react';
import { SimpleGrid as MantineSimpleGrid } from '@mantine/core';

// Equal-width columns via `repeat(cols, minmax(0, 1fr))`. Use this for card
// grids that should reflow at breakpoints (`cols={{ base: 1, sm: 2, md: 3 }}`);
// for a row of items that shrink instead of reflowing, use `Group` with `grow`.
// Unlike its `Box`/`Group`/`Stack` siblings this forwards its ref, so
// measurement hooks work against the grid element.
type Props = Omit<React.ComponentProps<typeof MantineSimpleGrid>, 'ref'>;

const SimpleGrid = (props: Props, ref: React.ForwardedRef<HTMLDivElement>) => (
  <MantineSimpleGrid ref={ref} {...props} />
);

export default forwardRef(SimpleGrid);
