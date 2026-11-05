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
import { Trans, useTranslation } from 'react-i18next';

import { Col, Panel, Row, Tabs } from 'components/bootstrap';

import TemplatesHelper from './TemplatesHelper';
import ConfigurationVariablesHelper from './ConfigurationVariablesHelper';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

type Props = {
  onVariableRename: (...args: any[]) => void;
};

const ConfigurationHelper = ({ onVariableRename }: Props) => {
  const { t } = useTranslation('sidecars');

  return (
    <Panel header={t('configuration_forms.configuration_helper.title')}>
      <Row className="row-sm">
        <Col md={12}>
          <Tabs defaultValue="runtime-variables">
            <Tabs.List>
              <Tabs.Tab value="runtime-variables">
                {t('configuration_forms.configuration_helper.tab_runtime_variables')}
              </Tabs.Tab>
              <Tabs.Tab value="variables">{t('configuration_forms.configuration_helper.tab_variables')}</Tabs.Tab>
              <Tabs.Tab value="reference">{t('configuration_forms.configuration_helper.tab_reference')}</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="runtime-variables">
              <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                {t('configuration_forms.configuration_helper.runtime_variables_help')}
              </p>
              <TemplatesHelper />
            </Tabs.Panel>
            <Tabs.Panel value="variables">
              <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                <Trans
                  t={t}
                  i18nKey="configuration_forms.configuration_helper.variables_help"
                  components={{ lineBreak: <br />, code: <code /> }}
                />
              </p>
              <ConfigurationVariablesHelper onVariableRename={onVariableRename} />
            </Tabs.Panel>
            <Tabs.Panel value="reference">
              <Row className="row-sm">
                <Col md={12}>
                  <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                    <Trans
                      t={t}
                      i18nKey="configuration_forms.configuration_helper.reference_help"
                      components={{ lineBreak: <br /> }}
                    />
                  </p>
                  <ul className={ConfigurationHelperStyle.ulStyle}>
                    <li>
                      <a
                        href="https://www.elastic.co/guide/en/beats/filebeat/current/index.html"
                        target="_blank"
                        rel="noopener noreferrer">
                        {t('configuration_forms.configuration_helper.filebeat_reference')}
                      </a>{' '}
                    </li>
                    <li>
                      <a
                        href="https://www.elastic.co/guide/en/beats/winlogbeat/current/index.html"
                        target="_blank"
                        rel="noopener noreferrer">
                        {t('configuration_forms.configuration_helper.winlogbeat_reference')}
                      </a>{' '}
                    </li>
                    <li>
                      <a
                        href="https://nxlog.co/docs/nxlog-ce/nxlog-reference-manual.html"
                        target="_blank"
                        rel="noopener noreferrer">
                        {t('configuration_forms.configuration_helper.nxlog_reference')}
                      </a>{' '}
                    </li>
                  </ul>
                </Col>
              </Row>
            </Tabs.Panel>
          </Tabs>
        </Col>
      </Row>
    </Panel>
  );
};

export default ConfigurationHelper;
