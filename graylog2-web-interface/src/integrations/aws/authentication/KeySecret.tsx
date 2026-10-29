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
import React from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import MaskedInput from 'integrations/aws/common/MaskedInput';

const StyledMaskedInput = styled(MaskedInput)`
  margin-bottom: 0;
`;

type KeySecretProps = {
  onChange: (...args: any[]) => void;
  awsKey?: any;
  awsSecret?: any;
  required?: boolean;
};

const KeySecret = ({ onChange, awsKey = undefined, awsSecret = undefined, required = true }: KeySecretProps) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <ValidatedInput
        id="awsAccessKey"
        type="text"
        label={t('aws_key_secret.access_key_label')}
        placeholder={t('aws_key_secret.access_key_placeholder')}
        onChange={onChange}
        fieldData={awsKey}
        autoComplete="off"
        maxLength={512}
        help={
          required
            ? t('aws_key_secret.access_key_help')
            : t('aws_key_secret.access_key_help_optional')
        }
        required={required}
      />

      <StyledMaskedInput
        id="awsSecretKey"
        label={t('aws_key_secret.secret_key_label')}
        placeholder={t('aws_key_secret.secret_key_placeholder')}
        onChange={onChange}
        fieldData={awsSecret}
        autoComplete="off"
        maxLength={512}
        help={
          required
            ? t('aws_key_secret.secret_key_help')
            : t('aws_key_secret.secret_key_help_optional')
        }
        required={required}
      />
    </>
  );
};

export default KeySecret;
