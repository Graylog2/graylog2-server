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

import { SearchForm } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';
import useProductName from 'brand-customization/useProductName';

const LAST_SEEN_QUERY_EXAMPLE = 'last_seen:<=2018-04-10';
const STATUS_QUERY_EXAMPLE = 'status:failing status:unknown';

const SidecarQueryHelper = () => {
  const productName = useProductName();
  const { t } = useTranslation('sidecars');

  const queryExamples = (
    <>
      <p>
        {t('common.sidecar_search_form.last_seen_example')}
        <br />
        <kbd>{LAST_SEEN_QUERY_EXAMPLE}</kbd>
        <br />
      </p>
      <p>
        <Trans t={t} i18nKey="common.sidecar_search_form.status_example" components={{ code: <code /> }} />
        <br />
        <kbd>{STATUS_QUERY_EXAMPLE}</kbd>
        <br />
      </p>
    </>
  );

  const fieldMap = {
    status: t('common.sidecar_search_form.field_status'),
    operating_system: t('common.sidecar_search_form.field_operating_system'),
    last_seen: t('common.sidecar_search_form.field_last_seen', { productName }),
    node_id: t('common.sidecar_search_form.field_node_id'),
    sidecar_version: t('common.sidecar_search_form.field_sidecar_version'),
  };

  return <QueryHelper entityName="sidecar" example={queryExamples} commonFields={['name']} fieldMap={fieldMap} />;
};

type Props = React.PropsWithChildren<{
  query: string;
  onSearch: (query: string) => void;
  onReset: () => void;
}>;

const SidecarSearchForm = ({ query, onSearch, onReset, children = undefined }: Props) => {
  const { t } = useTranslation('sidecars');

  return (
    <SearchForm
      query={query}
      onSearch={onSearch}
      onReset={onReset}
      placeholder={t('common.sidecar_search_form.placeholder')}
      queryHelpComponent={<SidecarQueryHelper />}
      topMargin={0}
      useLoadingState>
      {children}
    </SearchForm>
  );
};

export default SidecarSearchForm;
