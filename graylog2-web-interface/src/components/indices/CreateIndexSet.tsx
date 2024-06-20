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
import { useEffect } from 'react';

import AppConfig from 'util/AppConfig';
import { Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { useStore } from 'stores/connect';
import Routes from 'routing/Routes';
import { IndexSetsActions } from 'stores/indices/IndexSetsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import SelectIndexSetTemplateModal from 'components/indices/IndexSetTemplates/SelectIndexSetTemplateModal';
import { adjustFormat } from 'util/DateTime';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  showSelectTemplateModal: boolean,
  setShowSelectTemplateModal: (value: boolean) => void
}

const CreateIndexSet = ({
  showSelectTemplateModal,
  setShowSelectTemplateModal,
}: Props) => {
  const isCloud = AppConfig.isCloud();
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const {
    retentionStrategies,
    rotationStrategies,
    retentionStrategiesContext,
  } = useStore(IndicesConfigurationStore);

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }, []);

  const _saveConfiguration = (indexSetItem: IndexSet) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDICES.INDEX_SET_CREATED, {
      app_pathname: 'indexsets',
    });

    const copy = indexSetItem;

    copy.creation_date = adjustFormat(new Date(), 'internal');

    return IndexSetsActions.create(copy).then(() => {
      history.push(Routes.SYSTEM.INDICES.LIST);
    });
  };

  const isLoading = () => !rotationStrategies || !retentionStrategies;

  if (isLoading()) {
    return <Spinner />;
  }

  return (
    <>
      <IndexSetConfigurationForm retentionStrategiesContext={retentionStrategiesContext}
                                 rotationStrategies={rotationStrategies}
                                 retentionStrategies={retentionStrategies}
                                 submitButtonText="Create index set"
                                 submitLoadingText="Creating index set..."
                                 create
                                 cancelLink={Routes.SYSTEM.INDICES.LIST}
                                 onUpdate={_saveConfiguration} />
      {!isCloud && showSelectTemplateModal && (
        <SelectIndexSetTemplateModal show={showSelectTemplateModal} hideModal={() => setShowSelectTemplateModal(false)} />
      )}
    </>
  );
};

export default CreateIndexSet;
