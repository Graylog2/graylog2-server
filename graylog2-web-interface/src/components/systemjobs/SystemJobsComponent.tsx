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
import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import { SystemJobsList } from 'components/systemjobs';
import { Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useProductName from 'brand-customization/useProductName';
import useSystemJobs from 'components/systemjobs/useSystemJobs';

const SystemJobsComponent = () => {
  const { t } = useTranslation('system');
  const productName = useProductName();
  const jobs = useSystemJobs();
  const jobList = useMemo(
    () => (jobs ? Object.values(jobs).flatMap((jobsPerNode) => jobsPerNode?.jobs ?? []) : undefined),
    [jobs],
  );

  if (!jobList) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <h2>{t('system_jobs_component.title')}</h2>
        <p className="description">{t('system_jobs_component.description', { productName })}</p>

        <SystemJobsList jobs={jobList} />
      </Col>
    </Row>
  );
};

export default SystemJobsComponent;
