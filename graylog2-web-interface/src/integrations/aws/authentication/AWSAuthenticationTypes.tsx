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

type FieldConfig = {
  keyField?: string;
  secretField?: string;
  arnField?: string;
  clearFields?: string[];
};

type AWSAuthenticationTypesProps = {
  onChange: (...args: any[]) => void;
  fieldConfig?: FieldConfig;
};

const AWSAuthenticationTypes = ({ onChange, fieldConfig = {} }: AWSAuthenticationTypesProps) => {
  const { clearField, formData } = useContext(FormDataContext);

  // Default field configuration for CloudWatch (backward compatibility)
  const config = {
    keyField: 'awsCloudWatchAwsKey',
    secretField: 'awsCloudWatchAwsSecret',
    arnField: 'awsCloudWatchAssumeARN',
    clearFields: ['awsCloudWatchAwsKey', 'awsCloudWatchAwsSecret'],
    ...fieldConfig,
  };

  const { awsAuthenticationType } = formData;
  const awsKey = formData[config.keyField];
  const awsSecret = formData[config.secretField];
  const awsARN = formData[config.arnField];

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

    if (isType(AWS_AUTH_TYPES.automatic)) {
      config.clearFields.forEach((field) => clearField(field));
    }
  };

  return (
    <>
      {AppConfig.isCloud() ? (
        <KeySecret
          awsKey={awsKey}
          awsSecret={awsSecret}
          onChange={onChange}
          keyFieldId={config.keyField}
          secretFieldId={config.secretField}
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
                awsKey={awsKey}
                awsSecret={awsSecret}
                onChange={onChange}
                keyFieldId={config.keyField}
                secretFieldId={config.secretField}
              />
            )}
          </AuthWrapper>
        </>
      )}
      {config.arnField && <ARN awsARN={awsARN} onChange={onChange} arnFieldId={config.arnField} />}
    </>
  );
};

export default AWSAuthenticationTypes;
