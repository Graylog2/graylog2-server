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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { Button, ControlLabel, FormControl, FormGroup } from 'components/graylog';
import { Spinner } from 'components/common';

const StyledForm: StyledComponent<{}, void, HTMLFormElement> = styled.form`
  margin-top: 10px;
  &.form-inline > .form-group {
    margin-right: 10px;
    > input {
      width: 300px;
    }
  }
`;

type Props = {
  creatingToken: boolean,
  onCreate: (tokenName: string) => void,
};

const CreateTokenForm = ({ creatingToken, onCreate }: Props) => {
  const [tokenName, setTokenName] = useState('');

  const createToken = (event) => {
    event.preventDefault();
    onCreate(tokenName);
    setTokenName('');
  };

  return (
    <StyledForm className="form-inline" onSubmit={createToken}>
      <FormGroup controlId="create-token-input">
        <ControlLabel>Token Name</ControlLabel>
        <FormControl type="text"
                     placeholder="What is this token for?"
                     value={tokenName}
                     onChange={(event) => setTokenName(event.target.value)} />
      </FormGroup>
      <Button id="create-token"
              disabled={tokenName === '' || creatingToken}
              type="submit"
              bsStyle="primary">
        {(creatingToken ? <Spinner text="Creating..." /> : 'Create Token')}
      </Button>
    </StyledForm>
  );
};

CreateTokenForm.propTypes = {
  creatingToken: PropTypes.bool,
  onCreate: PropTypes.func.isRequired,
};

CreateTokenForm.defaultProps = {
  creatingToken: false,
};

export default CreateTokenForm;
