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
import { Progress as MantineProgress } from '@mantine/core';

const Root = React.forwardRef<HTMLDivElement, React.ComponentProps<typeof MantineProgress.Root>>((props, ref) => (
  <MantineProgress.Root ref={ref} {...props} />
));
const Section = React.forwardRef<HTMLDivElement, React.ComponentProps<typeof MantineProgress.Section>>((props, ref) => (
  <MantineProgress.Section ref={ref} {...props} />
));
const Label = ({ ...props }: React.ComponentProps<typeof MantineProgress.Label>) => (
  <MantineProgress.Label {...props} />
);

const Progress = { Root, Section, Label };

export default Progress;
