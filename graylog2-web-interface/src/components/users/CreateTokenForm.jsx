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

import { Button, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Spinner } from 'components/common';

const StyledForm: StyledComponent<{}, void, HTMLFormElement> = styled.form`
  margin-top: 10px;
`;

const StyledControlLabel: StyledComponent<{}, void, ControlLabel> = styled(ControlLabel)`
  margin-top: 8px;
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
    <StyledForm onSubmit={createToken}>
      <FormGroup>
        <Row>
          <Col sm={2}>
            <StyledControlLabel>Token Name</StyledControlLabel>
          </Col>
          <Col sm={4}>
            <FormControl id="create-token-input"
                         type="text"
                         value={tokenName}
                         onChange={(event) => setTokenName(event.target.value)} />
            <HelpBlock>Descriptive name for this Token.</HelpBlock>
          </Col>
          <Col sm={2}>
            <Button id="create-token"
                    disabled={tokenName === '' || creatingToken}
                    type="submit"
                    bsStyle="primary">
              {(creatingToken ? <Spinner text="Creating..." /> : 'Create Token')}
            </Button>
          </Col>
        </Row>
      </FormGroup>
      <hr />
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
