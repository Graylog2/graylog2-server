import React, { useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';

import { Row, Col, Button, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { FormSubmit } from 'components/common';

import BootstrapModalForm from '../bootstrap/BootstrapModalForm';

type Props = {
  pipeline?: PipelineType
  create?: boolean
  modal?: boolean
  save: (pipeline: PipelineType, callback: () => void) => void,
  onCancel?: () => void,
};

const emptyPipeline: PipelineType = {
  id: undefined,
  title: '',
  description: '',
  stages: [{ stage: 0, rules: [], match: '' }],
  source: '',
  created_at: '',
  modified_at: '',
};

const PipelineForm = ({
  pipeline = emptyPipeline, create = false, modal = true, save, onCancel = () => {},
}: Props) => {
  const currentUser = useCurrentUser();
  const [nextPipeline, setNextPipeline] = useState<PipelineType>(cloneDeep(pipeline));
  const [showModal, setShowModal] = useState<boolean>(false);

  const _openModal = () => {
    setShowModal(true);
  };

  const _onChange = ({ target }: React.ChangeEvent<HTMLInputElement>) => {
    setNextPipeline((currentPipeline) => ({ ...currentPipeline, [target.name]: getValueFromInput(target) }));
  };

  const _closeModal = () => {
    setShowModal(false);
  };

  const _onSaved = () => {
    _closeModal();

    if (create) {
      setNextPipeline(cloneDeep(pipeline));
    }
  };

  const _handleSubmit = (event: React.FormEvent) => {
    if (event) {
      event.preventDefault();
    }

    save(nextPipeline, _onSaved);
  };

  const content = (
    <fieldset>
      <Input type="text"
             id="title"
             name="title"
             label="Title"
             autoFocus
             required
             onChange={_onChange}
             help="Pipeline name."
             value={nextPipeline.title} />

      <Input type="text"
             id="description"
             name="description"
             label="Description"
             onChange={_onChange}
             help="Pipeline description."
             value={nextPipeline.description} />
    </fieldset>
  );

  if (modal) {
    return (
      <span>
        <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:edit')}
                onClick={_openModal}
                bsStyle="success">
          {create ? 'Add new pipeline' : 'Edit pipeline details'}
        </Button>
        <BootstrapModalForm show={showModal}
                            title={`${create ? 'Add new' : 'Edit'} pipeline ${nextPipeline.title}`}
                            data-telemetry-title={`${create ? 'Add new' : 'Edit'} pipeline`}
                            onSubmitForm={_handleSubmit}
                            onCancel={_closeModal}
                            submitButtonText={create ? 'Add pipeline' : 'Update pipeline'}>
          {content}
        </BootstrapModalForm>
      </span>
    );
  }

  return (
    <form onSubmit={_handleSubmit}>
      {content}
      <Row>
        <Col md={12}>
          <FormSubmit submitButtonText={create ? 'Create pipeline' : 'Update pipeline'} onCancel={onCancel} />
        </Col>
      </Row>
    </form>
  );
};

export default PipelineForm;
