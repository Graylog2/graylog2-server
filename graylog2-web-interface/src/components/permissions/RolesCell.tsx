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
import * as Immutable from 'immutable';

import { Label } from 'components/graylog';

const Td = styled.td`
  max-width: 300px;
`;

const Role = styled(Label)`
  margin-right: 5px;
  margin-bottom: 5px;
  display: inline-block;
  line-height: 15px;
`;

type Props = {
  roles: Immutable.Set<string>,
};

const RolesCell = ({ roles }: Props) => (
  <Td>
    {roles.map((role) => (
      <Role key={role} bsStyle={role === 'Admin' ? 'info' : 'default'}>
        {role}
      </Role>
    ))}
  </Td>
);

export default RolesCell;
