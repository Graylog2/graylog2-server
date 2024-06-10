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
import { useState, useMemo } from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { BootstrapModalForm, Button, ControlLabel, HelpBlock, FormGroup } from 'components/bootstrap';
import { SelectableList } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';
import useStreamPipelinesConnectionMutation from 'components/streams/hooks/useStreamPipelinesConnections';

type Props = {
  streamId: string,
  pipelines: Array<PipelineType>,
  connectedPipelines: any,
};
type FormattedPipelines = {
  value: string,
  label: string,
}

const formatPipelines = (pipelines: Array<PipelineType>): Array<FormattedPipelines> => pipelines
  .map((s) => ({ value: s.id, label: s.title }))
  .sort((s1, s2) => naturalSort(s1.label, s2.label));

const StreamPipelinesConnectionForm = ({ streamId, pipelines, connectedPipelines }: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const currentUser = useCurrentUser();
  const { onSaveStreamPipelinesConnection } = useStreamPipelinesConnectionMutation();
  const formattedConnectedPipelines = formatPipelines(connectedPipelines);
  const [updatedPipelines, setUpdatePipelines] = useState<Array<FormattedPipelines>>(formattedConnectedPipelines || []);
  const notConnectedPipelines = useMemo(() => pipelines.filter((s) => !updatedPipelines.some((cs) => cs.value.toLowerCase() === s.id.toLowerCase())), [pipelines, updatedPipelines]);

  const openModal = () => {
    setShowModal(true);
  };

  const onPipelineChange = (newPipelines: Array<FormattedPipelines>) => {
    setUpdatePipelines(newPipelines.sort((s1, s2) => naturalSort(s1.label, s2.label)));
  };

  const onCloseModal = () => {
    setShowModal(false);
  };

  const onSave = () => {
    onSaveStreamPipelinesConnection({ streamId, pipelineIds: updatedPipelines.map((p) => p.value) });
    setShowModal(false);
  };

  const pipelinesHelp = (
    <span>
      Select the pipelines you want to connect to this stream, or create one in the{' '}
      <Link to={Routes.SYSTEM.PIPELINES.OVERVIEW}>Pipelines page</Link>.
    </span>
  );

  return (
    <>
      <Button disabled={!isPermitted(currentUser.permissions, 'pipeline_connection:edit')}
              onClick={openModal}
              bsStyle="info">
        Edit pipelines connection
      </Button>
      {showModal && (
      <BootstrapModalForm show={showModal}
                          title={<span>Edit connections for <em>stream</em></span>}
                          onSubmitForm={onSave}
                          onCancel={onCloseModal}
                          submitButtonText="Update connections">
        <fieldset>
          <FormGroup id="pipelinesConnections">
            <ControlLabel>Pipelines</ControlLabel>
            <SelectableList options={formatPipelines(notConnectedPipelines)}
                            onChange={onPipelineChange}
                            selectedOptionsType="object"
                            selectedOptions={updatedPipelines} />
            <HelpBlock>{pipelinesHelp}</HelpBlock>
          </FormGroup>
        </fieldset>
      </BootstrapModalForm>
      )}
    </>
  );
};

export default StreamPipelinesConnectionForm;
