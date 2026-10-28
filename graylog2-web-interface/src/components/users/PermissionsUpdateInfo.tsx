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
import { Trans, useTranslation } from 'react-i18next';

import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import { Col, Row, Alert } from 'components/bootstrap';
import { Icon } from 'components/common';

const ShareLabel = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <b>
    <Icon name="person_add" /> {children}
  </b>
);

const DocsLink = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <DocumentationLink page={DocsHelper.PAGES.PERMISSIONS} text={children} />
);

const PermissionsUpdateInfo = () => {
  const { t } = useTranslation('users');

  return (
    <Row className="content">
      <Col xs={12}>
        <Alert bsStyle="info" title={t('permissions_info.title')}>
          <Trans
            t={t}
            i18nKey="permissions_info.text"
            components={{ shareLabel: <ShareLabel />, docsLink: <DocsLink /> }}
          />
        </Alert>
      </Col>
    </Row>
  );
};

export default PermissionsUpdateInfo;
