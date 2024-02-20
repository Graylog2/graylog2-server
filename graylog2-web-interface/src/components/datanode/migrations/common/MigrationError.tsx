import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/bootstrap';

type Props = {
  errorMessage: string,
}
const StyledAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const MigrationError = ({ errorMessage }: Props) => {
  if (!errorMessage) {
    return null;
  }

  return (
    <StyledAlert bsStyle="danger">
      {errorMessage}
    </StyledAlert>
  );
};

export default MigrationError;
