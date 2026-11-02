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
import React, { useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import { useTranslation } from 'react-i18next';

import { Row, Col, Button, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import type { PipelineType } from 'components/pipelines/types';
import { DEFAULT_PIPELINE } from 'components/pipelines/types';
import { FormSubmit } from 'components/common';
import usePermissions from 'hooks/usePermissions';

import BootstrapModalForm from '../bootstrap/BootstrapModalForm';

type Props = {
  pipeline?: PipelineType;
  create?: boolean;
  modal?: boolean;
  save: (pipeline: PipelineType, callback: () => void) => void;
  onCancel?: () => void;
  disableEdit?: boolean;
};

const PipelineForm = ({
  pipeline = DEFAULT_PIPELINE,
  create = false,
  modal = true,
  save,
  onCancel = () => {},
  disableEdit = false,
}: Props) => {
  const { t } = useTranslation('pipelines');
  const { isPermitted } = usePermissions();
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
      <Input
        type="text"
        id="title"
        name="title"
        label={t('form.title')}
        required
        onChange={_onChange}
        help={t('form.title_help')}
        value={nextPipeline.title}
      />

      <Input
        type="text"
        id="description"
        name="description"
        label={t('form.description')}
        onChange={_onChange}
        help={t('form.description_help')}
        value={nextPipeline.description}
      />
    </fieldset>
  );

  if (modal) {
    return (
      <span>
        <Button disabled={!isPermitted('pipeline:edit') || disableEdit} onClick={_openModal} bsStyle="primary">
          {create ? t('form.add_button') : t('form.edit_button')}
        </Button>
        <BootstrapModalForm
          show={showModal}
          title={
            create
              ? t('form.add_title', { title: nextPipeline.title })
              : t('form.edit_title', { title: nextPipeline.title })
          }
          data-telemetry-title={`${create ? 'Add new' : 'Edit'} pipeline`}
          onSubmitForm={_handleSubmit}
          onCancel={_closeModal}
          submitButtonText={create ? t('form.add_submit') : t('form.update_submit')}>
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
          <FormSubmit
            submitButtonText={create ? t('form.create_submit') : t('form.update_submit')}
            onCancel={onCancel}
          />
        </Col>
      </Row>
    </form>
  );
};

export default PipelineForm;
