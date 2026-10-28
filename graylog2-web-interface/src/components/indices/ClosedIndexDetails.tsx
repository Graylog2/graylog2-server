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
import { useQueryClient } from '@tanstack/react-query';

import { Alert, Button } from 'components/bootstrap';
import IndexRangeSummary from 'components/indices/IndexRangeSummary';
import { INDICES_QUERY_KEY, reopenIndex, deleteIndex } from 'hooks/useIndices';

type ClosedIndexDetailsProps = {
  indexName: string;
  indexRange?: any;
};

const ClosedIndexDetails = ({ indexName, indexRange = undefined }: ClosedIndexDetailsProps) => {
  const { t } = useTranslation('indexsets');
  const queryClient = useQueryClient();

  const _invalidate = () => queryClient.invalidateQueries({ queryKey: INDICES_QUERY_KEY });

  const _onReopen = () => {
    reopenIndex(indexName).then(_invalidate);
  };

  const _onDeleteIndex = () => {
    if (window.confirm(t('indices.closed_index.confirm_delete', { indexName }))) {
      deleteIndex(indexName).then(_invalidate);
    }
  };

  return (
    <div className="index-info">
      <IndexRangeSummary indexRange={indexRange} />
      <Alert bsStyle="info">{t('indices.closed_index.info')}</Alert>
      <hr style={{ marginBottom: '5', marginTop: '10' }} />
      <Button bsStyle="warning" bsSize="xs" onClick={_onReopen}>
        {t('indices.closed_index.reopen')}
      </Button>{' '}
      <Button bsStyle="danger" bsSize="xs" onClick={_onDeleteIndex}>
        {t('indices.closed_index.delete')}
      </Button>
    </div>
  );
};

export default ClosedIndexDetails;
