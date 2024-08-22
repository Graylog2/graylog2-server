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
import styled from 'styled-components';

import { Modal, Button } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import copyToClipboard from 'util/copyToClipboard';
import UserNotification from 'util/UserNotification';
import { saveRuleSourceCode } from 'hooks/useRuleBuilder';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { ModalButtonToolbar } from 'components/common';

import type { RuleBuilderRule } from './types';

const SourceCodeContainer = styled.div`
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-height: 500px;
`;

type Props = {
  show: boolean,
  onHide: () => void,
  onNavigateAway: (rule: RuleBuilderRule) => void,
  rule: RuleBuilderRule,
};

const ConvertToSourceCodeModal = ({ show, onHide, onNavigateAway, rule }: Props) => {
  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  return (
    <BootstrapModalWrapper showModal={show}
                           onHide={onHide}
                           bsSize="large">
      <Modal.Header closeButton>
        <Modal.Title>{rule.title || '<no title>'}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <pre>
          <SourceCodeContainer>
            {rule.source || '<no code>'}
          </SourceCodeContainer>
        </pre>
      </Modal.Body>
      <Modal.Footer>
        <ModalButtonToolbar>
          <Button type="button"
                  bsStyle="success"
                  onClick={async () => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.CREATE_NEW_RULE_FROM_CODE_CLICKED, {
                      app_pathname: getPathnameWithoutId(pathname),
                      app_section: 'convert-rule-builder-to-source-code-modal',
                      app_action_value: 'create-new-rule-from-code-button',
                    });

                    saveRuleSourceCode(rule.source || '');
                    await onNavigateAway(rule);
                    history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
                  }}>
            Create new Rule from Code
          </Button>
          <Button type="button"
                  bsStyle="info"
                  onClick={() => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.COPY_CODE_AND_CLOSE_CLICKED, {
                      app_pathname: getPathnameWithoutId(pathname),
                      app_section: 'convert-rule-builder-to-source-code-modal',
                      app_action_value: 'copy-rule-code-and-close-button',
                    });

                    copyToClipboard(rule.source);
                    UserNotification.success('Rule source code copied to clipboard!');
                    onHide();
                  }}>
            Copy & Close
          </Button>
        </ModalButtonToolbar>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default ConvertToSourceCodeModal;
