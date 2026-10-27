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
import React, { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PageHeader, Spinner } from 'components/common';
import { Row, Col, Button, BootstrapModalConfirm, Alert } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useGetPermissionsByScope from 'hooks/useScopePermissions';

import RuleBuilder from './rule-builder/RuleBuilder';
import RuleForm from './RuleForm';
import RuleHelper from './rule-helper/RuleHelper';
import { PipelineRulesContext } from './RuleContext';

import PipelinesPageNavigation from '../pipelines/PipelinesPageNavigation';

type Props = {
  create?: boolean;
  title?: string;
  isRuleBuilder?: boolean;
};

const Rule = ({ create = false, title = '', isRuleBuilder = false }: Props) => {
  const { t } = useTranslation('pipelines');
  const [showConfirmSourceCodeEditor, setShowConfirmSourceCodeEditor] = useState<boolean>(false);
  const { rule } = useContext(PipelineRulesContext);
  const { loadingScopePermissions, scopePermissions } = useGetPermissionsByScope(rule);
  const isManaged = scopePermissions && !scopePermissions?.is_mutable;
  const history = useHistory();
  const sendTelemetry = useSendTelemetry('pipeline-rules');

  if (loadingScopePermissions) {
    return <Spinner text={t('loading_rule')} />;
  }

  let pageTitle;

  if (create) {
    pageTitle = t('create_pipeline_rule');
  } else {
    pageTitle = (
      <span>
        {t('pipeline_rule', { title })}
      </span>
    );
  }

  return (
    <div>
      <PipelinesPageNavigation />
      <PageHeader
        title={pageTitle}
        actions={
          isRuleBuilder && create ? (
            <Button
              bsStyle="primary"
              bsSize="small"
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.USE_SOURCE_CODE_EDITOR_CLICKED, {
                  app_action_value: 'source-code-editor-button',
                });

                setShowConfirmSourceCodeEditor(true);
              }}>
              {t('use_source_code_editor')}
            </Button>
          ) : undefined
        }
        documentationLink={{
          title: t('rules_page_documentation_link'),
          path: DocsHelper.PAGES.PIPELINE_RULES,
        }}>
        <span>
          {t('rules_page_description')}
        </span>
      </PageHeader>
      {isRuleBuilder ? (
        <RuleBuilder />
      ) : (
        <Row className="content">
          <Col md={6}>
            {isManaged && <Alert bsStyle="warning">{t('rule_is_managed_warning')}</Alert>}
            <RuleForm create={create} isManaged={isManaged} />
          </Col>
          <Col md={6}>
            <RuleHelper />
          </Col>
        </Row>
      )}

      {showConfirmSourceCodeEditor && (
        <BootstrapModalConfirm
          showModal
          title={t('switch_to_source_code_editor')}
          onConfirm={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.SWITCH_TO_SOURCE_CODE_EDITOR_CONFIRM_CLICKED, {
              app_action_value: 'confirm-button',
            });

            history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
            setShowConfirmSourceCodeEditor(false);
          }}
          onCancel={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.SWITCH_TO_SOURCE_CODE_EDITOR_CANCEL_CLICKED, {
              app_action_value: 'cancel-button',
            });

            setShowConfirmSourceCodeEditor(false);
          }}>
          <div>{t('switch_to_source_code_editor_confirm_text')}</div>
          <div>{t('switch_to_source_code_editor_confirm_warning')}</div>
        </BootstrapModalConfirm>
      )}
    </div>
  );
};

export default Rule;
