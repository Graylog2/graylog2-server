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
import * as React from 'react';
import { useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm, IndicesPageNavigation } from 'components/indices';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { updateIndexSet } from 'stores/indices/IndexSetsStore';
import useSingleIndexSet from 'components/indices/hooks/useSingleIndexSet';
import useParams from 'routing/useParams';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import SelectIndexSetTemplateProvider from 'components/indices/IndexSetTemplates/contexts/SelectedIndexSetTemplateProvider';
import useIndicesConfiguration from 'hooks/useIndicesConfiguration';

const _saveConfiguration = (history: HistoryFunction, indexSet: IndexSet) =>
  updateIndexSet(indexSet).then(() => {
    history.push(Routes.SYSTEM.INDICES.LIST);
  });

const IndexSetConfigurationPage = () => {
  const { t } = useTranslation('indexsets');
  const { indexSetId } = useParams();
  const { data: indexSet } = useSingleIndexSet(indexSetId);
  const { retentionStrategies, rotationStrategies, retentionStrategiesContext } = useIndicesConfiguration();
  const history = useHistory();
  const { from } = useQuery();
  const sendTelemetry = useSendTelemetry('indexset');

  const formCancelLink = () => {
    if (from === 'details') {
      return Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id);
    }

    return Routes.SYSTEM.INDICES.LIST;
  };

  const isLoading = () => !indexSet || !rotationStrategies || !retentionStrategies || indexSetId !== indexSet.id;

  if (isLoading()) {
    return <Spinner />;
  }

  const saveConfiguration = (newIndexSet: IndexSet) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDICES.INDEX_SET_UPDATED, {
      app_pathname: 'indexsets',
    });

    return _saveConfiguration(history, newIndexSet);
  };

  return (
    <SelectIndexSetTemplateProvider>
      <DocumentTitle title={t('pages.configure_index_set')}>
        <IndicesPageNavigation />
        <div>
          <PageHeader
            title={t('pages.configure_index_set')}
            documentationLink={{
              title: t('overview.header.documentation_link'),
              path: DocsHelper.PAGES.INDEX_MODEL,
            }}>
            <span>{t('pages.configure_description')}</span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <IndexSetConfigurationForm
                indexSet={indexSet}
                retentionStrategiesContext={retentionStrategiesContext}
                rotationStrategies={rotationStrategies}
                retentionStrategies={retentionStrategies}
                submitButtonText={t('pages.update_index_set')}
                submitLoadingText={t('pages.updating_index_set')}
                cancelLink={formCancelLink()}
                onUpdate={saveConfiguration}
              />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    </SelectIndexSetTemplateProvider>
  );
};

export default IndexSetConfigurationPage;
