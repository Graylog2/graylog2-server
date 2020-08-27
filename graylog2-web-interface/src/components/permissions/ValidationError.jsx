// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { Alert } from 'components/graylog';
import { type ThemeInterface } from 'theme';
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
};

const ValidationError = ({ validationResult }: Props) => {
  const errors = validationResult.errors || {};

  return (
    <Container bsStyle="danger">
      <List>
        { Object.keys(errors).map((key) => (
          <li key={key}>{errors[key]}</li>
        ))}
      </List>
    </Container>
  );
};

export default ValidationError;
