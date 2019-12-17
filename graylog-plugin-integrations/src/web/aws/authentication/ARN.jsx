import React from 'react';
import PropTypes from 'prop-types';

import { Input } from 'components/bootstrap';

const ARN = ({ awsARN, onChange }) => {
  return (
    <Input id="awsCloudWatchAssumeARN"
           type="text"
           value={awsARN.value}
           onChange={onChange}
           label="AWS Assume Role (ARN)"
           help="Amazon Resource Name with required cross account permission"
           placeholder="arn:aws:sts::123456789012:assumed-role/some-role"
           maxLength="2048" />
  );
};

ARN.propTypes = {
  awsARN: PropTypes.shape({
    value: PropTypes.string,
  }),
  onChange: PropTypes.func.isRequired,
};

ARN.defaultProps = {
  awsARN: {
    value: '',
  },
};

export default ARN;
