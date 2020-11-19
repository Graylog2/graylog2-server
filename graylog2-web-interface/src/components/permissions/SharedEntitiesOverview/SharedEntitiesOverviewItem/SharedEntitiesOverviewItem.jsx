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

import { Link } from 'components/graylog/router';
import SharedEntity from 'logic/permissions/SharedEntity';
import { getShowRouteFromGRN } from 'logic/permissions/GRN';

import OwnersCell from './OwnersCell';

type Props = {
  capabilityTitle: string,
  sharedEntity: SharedEntity,
};

const SharedEntitiesOverviewItem = ({
  capabilityTitle,
  sharedEntity: {
    owners,
    title,
    type,
    id,
  },
}: Props) => (
  <tr key={title + type}>
    <td className="limited">
      <Link to={getShowRouteFromGRN(id)}>{title}</Link>
    </td>
    <td className="limited">{type}</td>
    <OwnersCell owners={owners} />
    <td className="limited">{capabilityTitle}</td>
  </tr>
);

export default SharedEntitiesOverviewItem;
