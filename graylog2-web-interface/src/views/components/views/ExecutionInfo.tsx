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
import numeral from 'numeral';
import { useTranslation } from 'react-i18next';

import { Timestamp } from 'components/common';

type Props = {
  duration: number | undefined;
  executedAt: string | undefined;
  total?: number | undefined;
  showTotal?: boolean;
  executionFinished: boolean;
};

const ExecutionInfo = ({ duration, executedAt, total = undefined, showTotal = true, executionFinished }: Props) => {
  const { t } = useTranslation('search');

  if (!executionFinished) {
    return <i>{t('execution_info.no_query')}</i>;
  }

  return (
    <i>
      {t('execution_info.query_executed', { duration: numeral(duration).format('0,0') })}{' '}
      <Timestamp dateTime={executedAt} />{' '}
      {showTotal && t('execution_info.total_results', { total: numeral(total).format('0,0') })}
    </i>
  );
};

export default ExecutionInfo;
