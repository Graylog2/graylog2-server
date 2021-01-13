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
import PropTypes from 'prop-types';
import React, { useRef, useState } from 'react';
import { cloneDeep } from 'lodash';

import { Row, Col, Button } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import { PipelineType } from 'stores/pipelines/PipelinesStore';

type Props = {
  pipeline: PipelineType,
  create: boolean,
  modal: boolean,
  save: (pipeline: PipelineType, callback: () => void) => void,
  onCancel?: () => void,
};

const PipelineForm = ({ pipeline, create, modal, save, onCancel }: Props) => {
  const modalRef = useRef<BootstrapModalForm>();
  const [nextPipeline, setNextPipeline] = useState<PipelineType>(cloneDeep(pipeline));

  const _openModal = () => {
    if (modalRef.current) {
      modalRef.current.open();
    }
  };

  const _onChange = ({ target }) => {
    setNextPipeline((currentPipeline) => ({ ...currentPipeline, [target.name]: getValueFromInput(target) }));
  };

  const _closeModal = () => {
    if (modalRef.current) {
      modalRef.current.close();
    }
  };

  const _onSaved = () => {
    _closeModal();

    if (create) {
      setNextPipeline(cloneDeep(pipeline));
    }
  };

  const _handleSubmit = (event) => {
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
        <Button onClick={_openModal}
                bsStyle="success">
          {create ? 'Add new pipeline' : 'Edit pipeline details'}
        </Button>
        <BootstrapModalForm ref={modalRef}
                            title={`${create ? 'Add new' : 'Edit'} pipeline ${nextPipeline.title}`}
                            onSubmitForm={_handleSubmit}
                            submitButtonText="Save">
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
          <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save</Button>
          <Button type="button" onClick={onCancel}>Cancel</Button>
        </Col>
      </Row>
    </form>
  );
};

PipelineForm.propTypes = {
  pipeline: PropTypes.object,
  create: PropTypes.bool,
  modal: PropTypes.bool,
  save: PropTypes.func.isRequired,
  onCancel: PropTypes.func,
};

PipelineForm.defaultProps = {
  modal: true,
  create: false,
  pipeline: {
    id: undefined,
    title: '',
    description: '',
    stages: [{ stage: 0, rules: [] }],
  },
  onCancel: () => {},
};

export default PipelineForm;
