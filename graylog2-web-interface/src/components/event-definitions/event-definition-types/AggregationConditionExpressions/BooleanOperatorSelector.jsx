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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Col, FormControl, FormGroup } from 'components/graylog';
import { Select } from 'components/common';

const OperatorSelector = styled.div(({ theme }) => css`
  margin-bottom: 15px;
  font-size: ${theme.fonts.size.body};
`);

const BooleanOperatorSelect = styled(({ isFirstElement, ...props }) => <FormGroup {...props} />)`
  width: 100px;
  margin-left: ${(props) => (props.isFirstElement ? '' : '1em')};
  margin-right: 1em;
`;

const BooleanOperatorSelector = ({ initialText, operator, onOperatorChange }) => {
  return (
    <Col md={12}>
      <OperatorSelector className="form-inline">
        {initialText && (
          <FormGroup>
            <FormControl.Static>{initialText} </FormControl.Static>
          </FormGroup>
        )}
        <BooleanOperatorSelect isFirstElement={!initialText}>
          <Select className="boolean-operator"
                  matchProp="label"
                  size="small"
                  onChange={onOperatorChange}
                  options={[
                    { label: 'all', value: '&&' },
                    { label: 'any', value: '||' },
                  ]}
                  value={operator}
                  clearable={false} />
        </BooleanOperatorSelect>
        <FormGroup>
          <FormControl.Static> of the following rules:</FormControl.Static>
        </FormGroup>
      </OperatorSelector>
    </Col>
  );
};

BooleanOperatorSelector.propTypes = {
  initialText: PropTypes.string,
  operator: PropTypes.string.isRequired,
  onOperatorChange: PropTypes.func.isRequired,
};

BooleanOperatorSelector.defaultProps = {
  initialText: '',
};

export default BooleanOperatorSelector;
