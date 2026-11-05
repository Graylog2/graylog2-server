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
import { useTranslation } from 'react-i18next';

import type UserOverview from 'logic/users/UserOverview';
import { DataTable } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

type Props = {
  adminUser: UserOverview;
  dataRowFormatter: (user: UserOverview) => React.ReactElement;
  headerCellFormatter: (header: string) => React.ReactElement;
  headers: Array<string>;
};

const SystemAdministratorOverview = ({ adminUser, dataRowFormatter, headers, headerCellFormatter }: Props) => {
  const { t } = useTranslation('users');
  const productName = useProductName();

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>{t('users_list.admin.title')}</h2>
        <p className="description">{t('users_list.admin.description', { productName })}</p>
        <DataTable
          id="users-overview"
          headers={headers}
          headerCellFormatter={headerCellFormatter}
          sortByKey="fullName"
          rows={[adminUser]}
          dataRowFormatter={dataRowFormatter}
          filterKeys={[]}
        />
      </Col>
    </Row>
  );
};

export default SystemAdministratorOverview;
