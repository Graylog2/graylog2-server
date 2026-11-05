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
import { Trans, useTranslation } from 'react-i18next';

type AgreeProps = {
  groupName: string;
  streamName: string;
};

const Agree = ({ groupName, streamName }: AgreeProps) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <p>{t('aws_kinesis_setup_agree.description')}</p>

      <ol>
        <li>
          <Trans t={t} i18nKey="aws_kinesis_setup_agree.step_stream" components={{ strong: <strong /> }} />
        </li>
        <li>
          <Trans
            t={t}
            i18nKey="aws_kinesis_setup_agree.step_role"
            values={{ groupName, streamName }}
            components={{ strong: <strong /> }}
          />
        </li>
        <li>{t('aws_kinesis_setup_agree.step_subscription')}</li>
      </ol>
    </>
  );
};

export default Agree;
