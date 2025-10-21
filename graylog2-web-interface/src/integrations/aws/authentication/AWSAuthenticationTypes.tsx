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
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { AWS_AUTH_TYPES } from 'integrations/aws/common/constants';
import AppConfig from 'util/AppConfig';

import KeySecret from './KeySecret';
import Automatic from './Automatic';
import ARN from './ARN';

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

  const { awsAuthenticationType, awsAccessKey, awsSecretKey, awsAssumeRoleARN } = formData;

  let defaultAuthTypeValue;

  if (AppConfig.isCloud()) {
    defaultAuthTypeValue = AWS_AUTH_TYPES.keysecret;
  } else {
    defaultAuthTypeValue = awsAuthenticationType ? awsAuthenticationType.value : AWS_AUTH_TYPES.automatic;
  }

  const [currentType, setCurrenType] = useState(defaultAuthTypeValue);

  useEffect(() => {
    onChange({ target: { name: 'awsAuthenticationType', value: defaultAuthTypeValue } });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const isType = (type) => currentType === type;

  const handleTypeChange = (e) => {
    setCurrenType(e.target.value);
    onChange({ target: { name: 'awsAuthenticationType', value: e.target.value } });

    if (e.target.value === AWS_AUTH_TYPES.automatic) {
      clearField('awsAccessKey');
      clearField('awsSecretKey');
    }
  };

  const isCloud = AppConfig.isCloud();

  return (
    <>
      {isCloud ? (
        <KeySecret 
          awsKey={awsAccessKey} 
          awsSecret={awsSecretKey} 
          onChange={onChange}
        />
      ) : (
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
              <KeySecret 
                awsKey={awsAccessKey} 
                awsSecret={awsSecretKey} 
                onChange={onChange}
              />
            )}
          </AuthWrapper>
        </>
      )}
      <ARN 
        awsARN={awsAssumeRoleARN} 
        onChange={onChange}
      />
    </>
  );
};

export default AWSAuthenticationTypes;
