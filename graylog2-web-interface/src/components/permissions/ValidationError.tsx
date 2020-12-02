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
import styled, { StyledComponent } from 'styled-components';
import { capitalize } from 'lodash';

import { Alert } from 'components/graylog';
import { ThemeInterface } from 'theme';
import { GranteesList } from 'logic/permissions/EntityShareState';
import ValidationResult from 'logic/permissions/ValidationResult';

const Container: StyledComponent<{}, ThemeInterface, Alert> = styled(Alert)`
  margin-top: 20px;
  max-height: 240px;
  overflow: auto;
`;

const List = styled.ul`
  list-style: initial;
  padding-left: 20px;

  ul {
    list-style: circle;
  }
`;

type Props = {
  validationResult: ValidationResult,
  availableGrantees: GranteesList,
};

const ValidationError = ({ validationResult, availableGrantees }: Props) => {
  const pastOwners = validationResult.errorContext.selectedGranteeCapabilities.map(
    (grn) => availableGrantees.find((grantee) => grantee.id === grn),
  );

  return (
    <Container bsStyle="danger">
      <List>
        <li>
          Removing the following owners will leave the entity ownerless: <br />
          {pastOwners.map((owner, key) => (
            <span key={owner?.id}>
              {capitalize(owner?.type)} <i>{owner?.title}</i>
              {key !== pastOwners.size - 1 && ', '}
            </span>
          ))}
        </li>
      </List>
    </Container>
  );
};

export default ValidationError;
