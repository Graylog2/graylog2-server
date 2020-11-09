// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { Alert } from 'components/graylog';

export const NotificationContainer: StyledComponent<{}, ThemeInterface, Alert> = styled(Alert)`
  margin-top: 10px;
  word-break: break-word;
`;

export const Title: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  font-weight: bold;
  margin-bottom: 5px;
`;

const ErrorsList = styled.ul(({ theme }) => `
  font-family: ${theme.fonts.family.monospace};
  list-style: initial;
  padding-left: 20px;
`);

type Props = {
  errors: Array<string>,
  message: string,
};

const ConnectionErrors = ({ errors, message }: Props) => (
  <NotificationContainer bsStyle="danger">
    <Title>{message}</Title>
    <ErrorsList>
      {errors.map((error) => {
        return <li key={String(error)}>{String(error)}</li>;
      })}
    </ErrorsList>
  </NotificationContainer>
);

ConnectionErrors.propTypes = {
  errors: PropTypes.arrayOf(PropTypes.string).isRequired,
  message: PropTypes.string,
};

ConnectionErrors.defaultProps = {
  message: 'There was an error',
};

export default ConnectionErrors;
