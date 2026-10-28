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

import { Alert, Table } from 'components/bootstrap';
import { StatusIcon } from 'components/common';
import { IndexerFailure } from 'components/indexers';

type Props = {
  failures: any[];
};

const IndexerFailuresList = ({ failures }: Props) => {
  const { t } = useTranslation('indexsets');

  if (failures.length === 0) {
    return (
      <Alert bsStyle="success">
        <StatusIcon active /> {t('indexers.failures.none_hurray')}
      </Alert>
    );
  }

  return (
    <div className="scrollable-table">
      <Table className="indexer-failures" condensed>
        <thead>
          <tr>
            <th style={{ width: 200 }}>{t('indexers.failures.timestamp')}</th>
            <th>{t('indexers.failures.index')}</th>
            <th>{t('indexers.failures.letter_id')}</th>
            <th>{t('indexers.failures.error_message')}</th>
          </tr>
        </thead>
        <tbody>
          {failures.map((failure) => (
            <IndexerFailure key={`indexer-failure-${failure.letter_id}`} failure={failure} />
          ))}
        </tbody>
      </Table>
    </div>
  );
};

export default IndexerFailuresList;
