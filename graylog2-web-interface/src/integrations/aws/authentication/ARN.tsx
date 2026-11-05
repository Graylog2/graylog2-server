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
import { useTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';

type ARNProps = {
  awsARN?: {
    value?: string;
  };
  awsExternalId?: {
    value?: string;
  };
  onChange: (...args: any[]) => void;
};

const ARN = ({
  awsARN = {
    value: '',
  },
  awsExternalId = {
    value: '',
  },
  onChange,
}: ARNProps) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <Input
        id="awsAssumeRoleARN"
        type="text"
        value={awsARN.value}
        onChange={onChange}
        label={t('aws_arn.assume_role_label')}
        help={t('aws_arn.assume_role_help')}
        placeholder={t('aws_arn.assume_role_placeholder')}
        maxLength={2048}
      />
      <Input
        id="awsExternalId"
        type="text"
        name="awsExternalId"
        value={awsExternalId.value}
        onChange={onChange}
        label={t('aws_arn.external_id_label')}
        help={t('aws_arn.external_id_help')}
      />
    </>
  );
};

export default ARN;
