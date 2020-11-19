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
// @flow strict
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  queryBarElements: Array<React.ComponentType<{}>>,
};

const QueryBarElements = ({ queryBarElements = [] }: Props) => (
  <>
    {queryBarElements
      // eslint-disable-next-line react/no-array-index-key
      .map((Component, idx) => <Component key={idx} />)}
  </>
);

const mapping = {
  queryBarElements: 'views.elements.queryBar',
};

export default withPluginEntities(QueryBarElements, mapping);
