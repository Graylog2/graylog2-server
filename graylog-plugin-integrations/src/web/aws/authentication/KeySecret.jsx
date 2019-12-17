import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import ValidatedInput from 'aws/common/ValidatedInput';
import MaskedInput from 'aws/common/MaskedInput';

const KeySecret = ({ onChange, awsKey, awsSecret }) => {
  return (
    <>
      <ValidatedInput id="awsCloudWatchAwsKey"
                      type="text"
                      label="AWS Access Key"
                      placeholder="AK****************"
                      onChange={onChange}
                      fieldData={awsKey}
                      autoComplete="off"
                      maxLength="512"
                      help='Your AWS Key should be a 20-character long, alphanumeric string that starts with the letters "AK".'
                      required />

      <StyledMaskedInput id="awsCloudWatchAwsSecret"
                         label="AWS Secret Key"
                         placeholder="***********"
                         onChange={onChange}
                         fieldData={awsSecret}
                         autoComplete="off"
                         maxLength="512"
                         help="Your AWS Secret is usually a 40-character long, base-64 encoded string."
                         required />
    </>
  );
};

const StyledMaskedInput = styled(MaskedInput)`
  margin-bottom: 0;
`;

KeySecret.propTypes = {
  onChange: PropTypes.func.isRequired,
  awsKey: PropTypes.object,
  awsSecret: PropTypes.object,
};

KeySecret.defaultProps = {
  awsKey: undefined,
  awsSecret: undefined,
};

export default KeySecret;
