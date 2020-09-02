// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { capitalize } from 'lodash';

import { Alert } from 'components/graylog';
import { type ThemeInterface } from 'theme';
import { type GranteesList } from 'logic/permissions/EntityShareState';
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
