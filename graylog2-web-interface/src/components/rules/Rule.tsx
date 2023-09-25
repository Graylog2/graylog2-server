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
import React, { useState } from 'react';

import { PageHeader } from 'components/common';
import { Row, Col, Button, BootstrapModalConfirm } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

import RuleBuilder from './rule-builder/RuleBuilder';
import RuleForm from './RuleForm';
import RuleHelper from './rule-helper/RuleHelper';

import PipelinesPageNavigation from '../pipelines/PipelinesPageNavigation';

type Props = {
  create: boolean,
  title: string,
  isRuleBuilder: boolean,
};

const Rule = ({ create, title, isRuleBuilder }: Props) => {
  const [showConfirmSourceCodeEditor, setShowConfirmSourceCodeEditor] = useState<boolean>(false);

  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  let pageTitle;

  if (create) {
    pageTitle = 'Create pipeline rule';
  } else {
    pageTitle = <span>Pipeline rule <em>{title}</em></span>;
  }

  return (
    <div>
      <PipelinesPageNavigation />
      <PageHeader title={pageTitle}
                  actions={(isRuleBuilder && create) ? (
                    <Button bsStyle="success"
                            bsSize="small"
                            onClick={() => {
                              sendTelemetry('Pipeline RuleBuilder Use Source Code Editor Clicked', {
                                app_pathname: getPathnameWithoutId(pathname),
                                app_section: 'pipeline-rules',
                                app_action_value: 'source-code-editor-button',
                              });

                              setShowConfirmSourceCodeEditor(true);
                            }}>
                      Use Source Code Editor
                    </Button>
                  ) : undefined}
                  documentationLink={{
                    title: 'Pipeline rules documentation',
                    path: DocsHelper.PAGES.PIPELINE_RULES,
                  }}>
        <span>
          Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list{' '}
          of actions.{' '}
          Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
        </span>
      </PageHeader>

      {isRuleBuilder ? (
        <RuleBuilder />
      ) : (
        <Row className="content">
          <Col md={6}>
            <RuleForm create={create} />
          </Col>
          <Col md={6}>
            <RuleHelper />
          </Col>
        </Row>
      )}

      {showConfirmSourceCodeEditor && (
        <BootstrapModalConfirm showModal
                               title="Switch to Source Code Editor"
                               onConfirm={() => {
                                 sendTelemetry('Pipeline RuleBuilder Switch to Source Code Editor Confirm Clicked', {
                                   app_pathname: getPathnameWithoutId(pathname),
                                   app_section: 'pipeline-rules',
                                   app_action_value: 'confirm-button',
                                 });

                                 history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
                                 setShowConfirmSourceCodeEditor(false);
                               }}
                               onCancel={() => {
                                 sendTelemetry('Pipeline RuleBuilder Switch to Source Code Editor Cancel Clicked', {
                                   app_pathname: getPathnameWithoutId(pathname),
                                   app_section: 'pipeline-rules',
                                   app_action_value: 'cancel-button',
                                 });

                                 setShowConfirmSourceCodeEditor(false);
                               }}>
          <div>You are about to leave this page and go to the Source Code Editor.</div>
          <div>Make sure you have no unsaved changes.</div>
        </BootstrapModalConfirm>
      )}
    </div>
  );
};

Rule.propTypes = {
  title: PropTypes.string,
  create: PropTypes.bool,
  isRuleBuilder: PropTypes.bool,
};

Rule.defaultProps = {
  title: '',
  create: false,
  isRuleBuilder: false,
};

export default Rule;
