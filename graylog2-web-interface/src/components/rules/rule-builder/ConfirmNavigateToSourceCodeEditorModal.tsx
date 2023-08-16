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

import { Modal, Button } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';

import type { RuleBuilderRule } from './types';

type Props = {
  show: boolean,
  rule: RuleBuilderRule,
  onHide: () => void,
  onSave: (rule: RuleBuilderRule) => void,
};

const ConfirmNavigateToSourceCodeEditorModal = ({ show, onHide, onSave, rule }: Props) => {
  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const handleClickSave = async () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'source-code-editor-modal',
      app_action_value: 'save-button',
    });

    const autoSavedTitle = `auto-saved ${new Date().toISOString()}`;

    await onSave({
      ...rule,
      title: rule.title || autoSavedTitle,
    });

    history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
  };

  const handleClickDontSave = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'source-code-editor-modal',
      app_action_value: 'dont-save-button',
    });

    history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
  };

  return (
    <BootstrapModalWrapper showModal={show}
                           onHide={onHide}
                           bsSize="large">
      <Modal.Header closeButton>
        <Modal.Title>You have unsaved changes</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        Do you want to save changes before navigating to Source Code Editor?
      </Modal.Body>
      <Modal.Footer>
        <Button type="button"
                bsStyle="success"
                onClick={handleClickSave}>
          Save
        </Button>
        <Button type="button"
                onClick={handleClickDontSave}>
          Don&apos;t save
        </Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default ConfirmNavigateToSourceCodeEditorModal;
