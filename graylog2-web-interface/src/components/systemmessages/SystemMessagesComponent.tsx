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
import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { Pagination, Spinner } from 'components/common';
import { SystemMessagesList } from 'components/systemmessages';
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import useProductName from 'brand-customization/useProductName';

const PER_PAGE = 30;

type SystemMessagesResponse = {
  total: number;
  messages: Array<{}>;
};

const fetchSystemMessages = (page: number): Promise<SystemMessagesResponse> => {
  const url = URLUtils.qualifyUrl(ApiRoutes.SystemMessagesApiController.all(page).url);

  return fetchPeriodically<SystemMessagesResponse>('GET', url);
};

const SystemMessagesComponent = () => {
  const { t } = useTranslation('system');
  const productName = useProductName();
  const [currentPage, setCurrentPage] = useState(1);

  const { data } = useQuery({
    queryKey: ['system', 'messages', currentPage],
    queryFn: () => fetchSystemMessages(currentPage),
    refetchInterval: 1000,
  });

  const content =
    data?.total != null && data?.messages ? (
      <div>
        <SystemMessagesList messages={data.messages} />

        <nav style={{ textAlign: 'center' }}>
          <Pagination
            totalPages={Math.ceil(data.total / PER_PAGE)}
            currentPage={currentPage}
            onChange={setCurrentPage}
          />
        </nav>
      </div>
    ) : (
      <Spinner />
    );

  return (
    <Row className="content">
      <Col md={12}>
        <h2>{t('system_messages_component.title')}</h2>

        <p className="description">{t('system_messages_component.description', { productName })}</p>

        {content}
      </Col>
    </Row>
  );
};

export default SystemMessagesComponent;
