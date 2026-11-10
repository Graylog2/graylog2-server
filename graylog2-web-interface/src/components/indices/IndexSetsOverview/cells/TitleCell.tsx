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
import styled from 'styled-components';

import Routes from 'routing/Routes';
import { Link } from 'components/common';
import { Label } from 'components/bootstrap';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

const StatusLabel = styled(Label)`
  margin-left: 5px;
  vertical-align: inherit;
`;

type Props = {
  indexSet: IndexSet;
};

const TitleCell = ({ indexSet }: Props) => (
  <span>
    <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>{indexSet.title}</Link>
    {indexSet.default && (
      <StatusLabel bsStyle="primary" bsSize="xsmall">
        Default
      </StatusLabel>
    )}
    {!indexSet.writable && (
      <StatusLabel bsStyle="info" bsSize="xsmall">
        Read only
      </StatusLabel>
    )}
  </span>
);

export default TitleCell;
