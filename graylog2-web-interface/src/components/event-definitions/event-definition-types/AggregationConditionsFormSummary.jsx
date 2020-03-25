import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Button, Panel } from 'components/graylog';
import { Icon } from 'components/common';
import AggregationConditionSummary from './AggregationConditionSummary';

const StyledPanel = styled(Panel)`
  margin-top: 10px;
`;

const StyledButton = styled(Button)`
  margin-left: 15px;
  vertical-align: baseline;
`;

const AggregationConditionsFormSummary = (props) => {
  const { conditions, series, expressionValidation, showInlineValidation, toggleShowValidation } = props;

  return (
    <div>
      <StyledPanel header="Condition summary">
        {expressionValidation.isValid
          ? <p className="text-success"><Icon name="check-square" />&nbsp;Condition is valid</p>
          : (
            <p className="text-danger">
              <Icon name="exclamation-triangle" />&nbsp;Condition is not valid
              <StyledButton bsSize="xsmall" onClick={toggleShowValidation}>
                {showInlineValidation ? 'Hide errors' : 'Show errors'}
              </StyledButton>
            </p>
          )
          }
        <b>Preview:</b> <AggregationConditionSummary series={series} conditions={conditions} />
      </StyledPanel>
    </div>
  );
};

AggregationConditionsFormSummary.propTypes = {
  conditions: PropTypes.object.isRequired,
  series: PropTypes.array.isRequired,
  expressionValidation: PropTypes.object,
  showInlineValidation: PropTypes.bool,
  toggleShowValidation: PropTypes.func.isRequired,
};

AggregationConditionsFormSummary.defaultProps = {
  expressionValidation: { isValid: true },
  showInlineValidation: false,
};

export default AggregationConditionsFormSummary;
