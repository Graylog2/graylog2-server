import React, { useContext, useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import { AWS_AUTH_TYPES } from 'integrations/aws/common/constants';
import FormDataContext from 'integrations/contexts/FormDataContext';

import { Input } from 'components/bootstrap';
import AppConfig from 'util/AppConfig';

import Automatic from './Automatic.jsx';

import ValidatedInput from '../common/ValidatedInput';

const AuthWrapper = styled.div(
  ({ theme }) => css`
    margin: 0 0 21px 9px;
    padding: 3px 0 3px 21px;
    border-left: 3px solid ${theme.colors.gray[80]};
  `,
);

type AWSAuthenticationTypesProps = {
  onChange: (...args: any[]) => void;
};

const AWSAuthenticationTypes = ({ onChange }: AWSAuthenticationTypesProps) => {
  const { clearField, formData } = useContext(FormDataContext);

  const { awsAuthenticationType } = formData;

  let defaultAuthTypeValue;

  if (AppConfig.isCloud()) {
    defaultAuthTypeValue = AWS_AUTH_TYPES.keysecret;
  } else {
    defaultAuthTypeValue = awsAuthenticationType ? awsAuthenticationType.value : AWS_AUTH_TYPES.automatic;
  }

  const [currentType, setCurrentType] = useState(defaultAuthTypeValue);

  useEffect(() => {
    onChange({ target: { name: 'awsAuthenticationType', value: defaultAuthTypeValue } });
  }, [defaultAuthTypeValue, onChange]);

  const isType = (type) => currentType === type;

  const handleTypeChange = (e) => {
    setCurrentType(e.target.value);
    onChange({ target: { name: 'awsAuthenticationType', value: e.target.value } });

    if (isType(AWS_AUTH_TYPES.automatic)) {
      clearField('awsAccessKey');
      clearField('awsSecret');
    }
  };

  return (
    <>
      <Input
        type="select"
        name="awsAuthType"
        id="awsAuthType"
        onChange={handleTypeChange}
        label="AWS Authentication Type"
        defaultValue={currentType}>
        {Object.keys(AWS_AUTH_TYPES).map((type) => (
          <option value={AWS_AUTH_TYPES[type]} key={`option-${type}`}>
            {AWS_AUTH_TYPES[type]}
          </option>
        ))}
      </Input>

      <AuthWrapper>
        {isType(AWS_AUTH_TYPES.automatic) && <Automatic />}
        {isType(AWS_AUTH_TYPES.keysecret) && (
          <>
            <ValidatedInput
              className=""
              id="awsAccessKey"
              type="text"
              onChange={onChange}
              fieldData={formData.awsAccessKey}
              label="AWS Access key"
              help="The AWS IAM Access key ID with sufficient permission to your SQS Queue."
              required
            />
            <ValidatedInput
              className=""
              id="awsSecret"
              type="password"
              onChange={onChange}
              fieldData={formData.awsSecret}
              label="AWS Secret"
              help="The AWS secret key that application uses to access the SQS Queue."
              required
            />
          </>
        )}
      </AuthWrapper>
    </>
  );
};

export default AWSAuthenticationTypes;
