import React, { useCallback, useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';

import { FormDataContext } from 'aws/context/FormData';
import { AWS_AUTH_TYPES } from 'aws/common/constants';

import KeySecret from './KeySecret';
import Automatic from './Automatic';
import ARN from './ARN';

const AWSAuthenticationTypes = ({ onChange }) => {
  const { clearField, formData } = useContext(FormDataContext);

  const {
    awsAuthenticationType,
    awsCloudWatchAwsSecret,
    awsCloudWatchAwsKey,
    awsCloudWatchAssumeARN,
  } = formData;

  const defaultAuthTypeValue = awsAuthenticationType ? awsAuthenticationType.value : AWS_AUTH_TYPES.automatic;
  const [currentType, setCurrenType] = useState(defaultAuthTypeValue);

  useEffect(() => {
    onChange({ target: { name: 'awsAuthenticationType', value: defaultAuthTypeValue } });
  }, []);

  const AuthWrapper = useCallback(styled.div(({ theme }) => css`
    margin: 0 0 21px 9px;
    padding: 3px 0 3px 21px;
    border-left: 3px solid ${theme.color.secondary.tre};
  `), []);

  const isType = (type) => {
    return currentType === type;
  };

  const handleTypeChange = (e) => {
    setCurrenType(e.target.value);
    onChange({ target: { name: 'awsAuthenticationType', value: e.target.value } });

    if (isType(AWS_AUTH_TYPES.automatic)) {
      clearField('awsCloudWatchAwsKey');
      clearField('awsCloudWatchAwsSecret');
    }
  };

  return (
    <>
      <Input type="select"
             name="awsAuthType"
             id="awsAuthType"
             onChange={handleTypeChange}
             label="AWS Authentication Type"
             defaultValue={currentType}>
        {Object.keys(AWS_AUTH_TYPES).map(type => (
          <option value={AWS_AUTH_TYPES[type]}
                  key={`option-${type}`}>
            {AWS_AUTH_TYPES[type]}
          </option>
        ))}
      </Input>

      <AuthWrapper>
        {isType(AWS_AUTH_TYPES.automatic) && <Automatic />}

        {isType(AWS_AUTH_TYPES.keysecret) && (
          <KeySecret awsKey={awsCloudWatchAwsKey}
                     awsSecret={awsCloudWatchAwsSecret}
                     onChange={onChange} />
        )}
      </AuthWrapper>

      <ARN awsARN={awsCloudWatchAssumeARN} onChange={onChange} />
    </>
  );
};

AWSAuthenticationTypes.propTypes = {
  onChange: PropTypes.func.isRequired,
};

export default AWSAuthenticationTypes;
