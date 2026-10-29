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
import { Trans, useTranslation } from 'react-i18next';

import { Table } from 'components/bootstrap';
import { Icon } from 'components/common';
import { SectionTitle, SectionNote } from 'integrations/aws/common/sharedStyles';

const StyledTable = styled(Table)`
  margin: 0;
`;

const DocsAnchor = ({ href, children }: { href: string; children?: React.ReactNode }) => (
  <a href={href} target="_blank" rel="noopener noreferrer">
    {children}
  </a>
);

const Automatic = () => {
  const { t } = useTranslation('integrations');

  return (
    <StyledTable condensed>
      <thead>
        <tr>
          <td colSpan={2}>
            <SectionTitle>{t('aws_automatic.section_title')}</SectionTitle>
          </td>
        </tr>
      </thead>

      <tbody>
        <tr>
          <th>{t('aws_automatic.env_variables_label')}</th>
          <td>
            <Trans
              t={t}
              i18nKey="aws_automatic.env_variables_value"
              components={{ code: <code /> }}
            />
          </td>
        </tr>
        <tr>
          <th>{t('aws_automatic.java_properties_label')}</th>
          <td>
            <Trans
              t={t}
              i18nKey="aws_automatic.java_properties_value"
              components={{ code: <code /> }}
            />
          </td>
        </tr>
        <tr>
          <th>{t('aws_automatic.profiles_label')}</th>
          <td>
            <Trans
              t={t}
              i18nKey="aws_automatic.profiles_value"
              components={{ code: <code /> }}
            />
          </td>
        </tr>
        <tr>
          <th>{t('aws_automatic.ecs_label')}</th>
          <td>
            <Trans
              t={t}
              i18nKey="aws_automatic.ecs_value"
              components={{ code: <code /> }}
            />
          </td>
        </tr>
        <tr>
          <th>{t('aws_automatic.instance_label')}</th>
          <td>{t('aws_automatic.instance_value')}</td>
        </tr>
      </tbody>

      <tfoot>
        <tr>
          <td colSpan={2}>
            <SectionNote>
              <Trans
                t={t}
                i18nKey="aws_automatic.more_info"
                components={{
                  docsLink: (
                    <DocsAnchor href="https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html" />
                  ),
                  openIcon: <Icon name="open_in_new" />,
                }}
              />
            </SectionNote>
          </td>
        </tr>
      </tfoot>
    </StyledTable>
  );
};

export default Automatic;
