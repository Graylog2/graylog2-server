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

import { Panel } from 'components/bootstrap';

import styles from './FilterPreview.css';

type Props = React.PropsWithChildren<{
  hasError?: boolean;
}>;
const FilterPreviewResults = ({ children = undefined, hasError = false }: Props) => (
  <Panel className={styles.filterPreview} bsStyle={hasError ? 'danger' : 'default'}>
    <Panel.Heading>
      <Panel.Title>Filter Preview</Panel.Title>
    </Panel.Heading>
    <Panel.Body>{children}</Panel.Body>
  </Panel>
);

export default FilterPreviewResults;
