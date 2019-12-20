import React from 'react';
import PropTypes from 'prop-types';
import { Col, FormControl, FormGroup } from 'components/graylog';

import { Select } from 'components/common';

const BooleanOperatorSelector = ({ initialText, operator, onOperatorChange }) => {
  return (
    <Col md={12}>
      <div className="form-inline" style={{ marginBottom: '15px', fontSize: '14px' }}>
        {initialText && (
          <FormGroup>
            <FormControl.Static>{initialText} </FormControl.Static>
          </FormGroup>
        )}
        <FormGroup style={{ width: '100px', marginLeft: '1em', marginRight: '1em' }}>
          <Select className="boolean-operator"
                  matchProp="label"
                  size="small"
                  onChange={onOperatorChange}
                  options={[
                    { label: 'all', value: '&&' },
                    { label: 'any', value: '||' },
                  ]}
                  value={operator} />
        </FormGroup>
        <FormGroup>
          <FormControl.Static> of the following rules:</FormControl.Static>
        </FormGroup>
      </div>
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
