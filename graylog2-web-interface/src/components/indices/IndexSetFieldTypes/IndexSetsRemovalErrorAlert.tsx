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

import type {
  RemovalResponse,
  IndexSetResponse,
} from 'components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation';
import { Alert } from 'components/bootstrap';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

const IndexSetRemovalErrors = ({
  errors,
  failures,
  title,
}: {
  errors: IndexSetResponse['errors'];
  failures: IndexSetResponse['failures'];
  title: string;
}) => {
  const { t } = useTranslation('indexsets');

  return (
    <div>
      <h4>
        <b>{title}:</b>
      </h4>
      <ul>
        {!!errors.length && (
          <li>
            <h5>
              <b>{t('field_types.general_errors')}</b>
            </h5>
            <ul>
              {errors.map((error) => (
                <li key={error}>
                  <i>{error}</i>
                </li>
              ))}
            </ul>
          </li>
        )}
        {!!failures.length && (
          <li>
            <h4>
              <b>{t('field_types.field_errors')}</b>
            </h4>
            <ul>
              {failures.map(({ entityId, failureExplanation }) => (
                <li key={entityId}>
                  <b>{entityId}</b> - <i>{failureExplanation}</i>
                </li>
              ))}
            </ul>
          </li>
        )}
      </ul>
    </div>
  );
};

const IndexSetsRemovalErrorAlert = ({
  removalResponse,
  indexSets,
}: {
  removalResponse: RemovalResponse;
  indexSets: Record<string, IndexSet>;
}) => {
  const { t } = useTranslation('indexsets');

  return (
    <Alert bsStyle="danger" title={t('field_types.removal_failed_title')}>
      {Object.values(removalResponse).map(({ indexSetId, failures, errors }) => (
        <IndexSetRemovalErrors
          key={indexSetId}
          failures={failures}
          errors={errors}
          title={indexSets[indexSetId].title}
        />
      ))}
    </Alert>
  );
};

export default IndexSetsRemovalErrorAlert;
