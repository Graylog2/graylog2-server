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

import { Table } from 'components/bootstrap';

const TAGS_EXAMPLE_IF_OPEN = '<#if sidecar.tags.webserver??>';
const TAGS_EXAMPLE_PATH = '- /var/log/apache/*.log';
const TAGS_EXAMPLE_IF_CLOSE = '</#if>';

const TemplatesHelper = () => {
  const { t } = useTranslation('sidecars');
  const _buildVariableName = (name) => `\${sidecar.${name}}`;

  return (
    <div>
      <Table responsive>
        <thead>
          <tr>
            <th>{t('configuration_forms.templates_helper.name')}</th>
            <th>{t('configuration_forms.templates_helper.description')}</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <code>{_buildVariableName('operatingSystem')}</code>
            </td>
            <td>
              <Trans
                t={t}
                i18nKey="configuration_forms.templates_helper.operating_system"
                components={{ code: <code /> }}
              />
            </td>
          </tr>
          <tr>
            <td>
              <code>{_buildVariableName('nodeName')}</code>
            </td>
            <td>{t('configuration_forms.templates_helper.node_name')}</td>
          </tr>
          <tr>
            <td>
              <code>{_buildVariableName('nodeId')}</code>
            </td>
            <td>{t('configuration_forms.templates_helper.node_id')}</td>
          </tr>
          <tr>
            <td>
              <code>{_buildVariableName('sidecarVersion')}</code>
            </td>
            <td>{t('configuration_forms.templates_helper.sidecar_version')}</td>
          </tr>
          <tr>
            <td>
              <code>{_buildVariableName('spoolDir')}</code>
            </td>
            <td>{t('configuration_forms.templates_helper.spool_dir')}</td>
          </tr>
          <tr>
            <td>
              <code>{_buildVariableName('tags.<tag>')}</code>
            </td>
            <td>
              {t('configuration_forms.templates_helper.tags')}
              <br />
              <code>
                {' '}
                {TAGS_EXAMPLE_IF_OPEN}
                <br />
                &nbsp;&nbsp;{TAGS_EXAMPLE_PATH}
                <br />
                {TAGS_EXAMPLE_IF_CLOSE}{' '}
              </code>
            </td>
          </tr>
        </tbody>
      </Table>
    </div>
  );
};

export default TemplatesHelper;
