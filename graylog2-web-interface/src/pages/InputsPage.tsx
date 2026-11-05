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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Link } from 'components/common';
import PageNavigation from 'components/common/PageNavigation';
import AppConfig from 'util/AppConfig';
import DocsHelper from 'util/DocsHelper';
import useProductName from 'brand-customization/useProductName';
import { InputsOverview } from 'components/inputs/InputsOveriew';
import useInputTypes from 'hooks/useInputTypes';
import Routes from 'routing/Routes';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';
import InputsNotifications from 'components/inputs/InputsNotifications';

const isCloud = AppConfig.isCloud();
export const INPUTS_PAGE_NAV_TITLE = 'Inputs';

const InputsPage = () => {
  const { t } = useTranslation('inputs');
  const productName = useProductName();
  const { data: inputTypes, isLoading: isLoadingInputTypes } = useInputTypes();
  const { data: inputTypeDescriptions, isLoading: isLoadingInputTypesDescriptions } = useInputTypesDescriptions();

  if (isLoadingInputTypes || isLoadingInputTypesDescriptions) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('pages.inputs')}>
      <Row>
        <PageNavigation page={INPUTS_PAGE_NAV_TITLE} />
      </Row>
      <PageHeader
        title={t('pages.inputs')}
        documentationLink={{
          title: t('pages.docs'),
          path: DocsHelper.PAGES.INPUTS,
        }}>
        {isCloud ? (
          <>
            <p>{t('pages.cloud_description', { productName })}</p>
            <p>
              <Trans
                t={t}
                i18nKey="pages.cloud_forwarder_hint"
                components={{ forwarderLink: <Link to={Routes.SYSTEM.FORWARDERS} /> }}
              />
            </p>
          </>
        ) : (
          <span>{t('pages.description', { productName })}</span>
        )}
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <InputsNotifications />

          <InputsOverview inputTypeDescriptions={inputTypeDescriptions} inputTypes={inputTypes} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default InputsPage;
