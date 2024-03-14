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
