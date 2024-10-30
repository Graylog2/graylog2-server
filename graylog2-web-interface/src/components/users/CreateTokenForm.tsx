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
import React, { useState } from 'react';
import styled from 'styled-components';

import { Button, ControlLabel, FormControl, FormGroup } from 'components/bootstrap';
import { Spinner } from 'components/common';

const StyledForm = styled.form`
  margin-top: 10px;

  &.form-inline > .form-group {
    margin-right: 10px;

    > input {
      width: 300px;
    }
  }
`;

type Props = {
  creatingToken?: boolean
  disableForm?: boolean
  onCreate: (tokenName: string) => void,
};

const CreateTokenForm = ({ creatingToken = false, disableForm = false, onCreate }: Props) => {
  const [tokenName, setTokenName] = useState('');

  const createToken = (event: React.SyntheticEvent) => {
    event.preventDefault();
    onCreate(tokenName);
    setTokenName('');
  };

  return (
    <StyledForm className="form-inline" onSubmit={createToken}>
      <FormGroup controlId="create-token-input">
        <ControlLabel>Token Name</ControlLabel>
        <FormControl type="text"
                     disabled={disableForm}
                     placeholder="What is this token for?"
                     value={tokenName}
                     onChange={(event) => setTokenName((event.target as HTMLInputElement).value)} />
      </FormGroup>
      <Button id="create-token"
              disabled={disableForm || tokenName === '' || creatingToken}
              type="submit"
              bsStyle="primary">
        {(creatingToken ? <Spinner text="Creating..." /> : 'Create Token')}
      </Button>
    </StyledForm>
  );
};

export default CreateTokenForm;
