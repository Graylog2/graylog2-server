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
import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import { FormDataContext } from 'aws/context/FormData';
import { AWS_AUTH_TYPES } from 'aws/common/constants';

import KeySecret from './KeySecret';
import Automatic from './Automatic';
import ARN from './ARN';

const AuthWrapper = styled.div(({ theme }) => css`
  margin: 0 0 21px 9px;
  padding: 3px 0 3px 21px;
  border-left: 3px solid ${theme.colors.gray[80]};
`);

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
        {Object.keys(AWS_AUTH_TYPES).map((type) => (
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
