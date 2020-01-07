import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Col, FormControl, FormGroup } from 'components/graylog';

import { Select } from 'components/common';

const OperatorSelector = styled.div`
  margin-bottom: 15px;
  font-size: 14px;
`;

const BooleanOperatorSelect = styled(({ isFirstElement, ...props }) => <FormGroup {...props} />)`
  width: 100px;
  margin-left: ${props => (props.isFirstElement ? '' : '1em')};
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
                  value={operator} />
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
