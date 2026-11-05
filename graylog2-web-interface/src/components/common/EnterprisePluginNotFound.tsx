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
import * as React from 'react';
import { useTranslation, Trans } from 'react-i18next';
import styled from 'styled-components';

import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import Panel from 'components/bootstrap/Panel';

import Icon from './Icon';

const Header = styled(Panel.Title)`
  display: flex;
  align-items: center;
`;

const HeaderIcon = styled(Icon)`
  margin-right: 5px;
  margin-top: -1px;
`;

type Props = {
  featureName: string;
  wrapperClassName?: string | null | undefined;
};

// `Trans` provides the link texts as children.
const LicenseLink = ({ children = undefined }: React.PropsWithChildren) => (
  <a href="https://www.graylog.org/products/enterprise" rel="noopener noreferrer" target="_blank">
    {children}
  </a>
);

const EnterpriseSetupLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page={DocsHelper.PAGES.ENTERPRISE_SETUP} text={children} />
);

const EnterprisePluginNotFound = ({ featureName, wrapperClassName = 'no-bm' }: Props) => {
  const { t } = useTranslation('common');

  return (
    <Panel bsStyle="info" className={wrapperClassName}>
      <Panel.Heading>
        <Header>
          <HeaderIcon name="enterprise" />
          {t('enterprise_plugin_not_found.title')}
        </Header>
      </Panel.Heading>
      <Panel.Body>
        <Trans
          t={t}
          i18nKey="enterprise_plugin_not_found.description"
          values={{ featureName }}
          components={{
            bold: <b />,
            licenseLink: <LicenseLink />,
            docsLink: <EnterpriseSetupLink />,
          }}
        />
      </Panel.Body>
    </Panel>
  );
};

export default EnterprisePluginNotFound;
