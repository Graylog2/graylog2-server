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

import ErrorPage from 'components/errors/ErrorPage';
import { Link } from 'components/common';
import Routes from 'routing/Routes';

type Props = {
  displayPageLayout?: boolean;
};

const NotFoundPage = ({ displayPageLayout = true }: Props) => {
  const { t } = useTranslation('pages');
  const description = (
    <>
      <p>{t('not_found.description')}</p>
      <p>
        <Trans t={t} i18nKey="not_found.back_hint" components={{ mainPageLink: <Link to={Routes.WELCOME} /> }} />
      </p>
    </>
  );

  return <ErrorPage title={t('not_found.title')} description={description} displayPageLayout={displayPageLayout} />;
};

export default NotFoundPage;
