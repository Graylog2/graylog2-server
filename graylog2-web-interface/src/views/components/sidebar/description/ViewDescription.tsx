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

import type QueryResult from 'views/logic/QueryResult';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import useViewType from 'views/hooks/useViewType';
import useViewMetadata from 'views/hooks/useViewMetadata';
import { MoreActionsIcon } from 'components/common/MoreActions';

import SearchResultOverview from './SearchResultOverview';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

type Props = {
  results: QueryResult;
};

const ViewDescription = ({ results }: Props) => {
  const { t } = useTranslation('search');
  const viewMetadata = useViewMetadata();
  const isAdHocSearch = !viewMetadata.id;
  const viewType = useViewType();
  const viewTypeLabel = viewType ? ViewTypeLabel({ type: viewType }) : '';

  const resultsSection = (
    <>
      <SectionSubheadline>{t('sidebar_description.execution')}</SectionSubheadline>

      <SearchResultOverview results={results} />
    </>
  );

  if (isAdHocSearch) {
    return (
      <>
        <SectionInfo>{t('sidebar_description.ad_hoc_info')}</SectionInfo>
        {resultsSection}
      </>
    );
  }

  return (
    <>
      {(!viewMetadata.summary || !viewMetadata.description) && (
        <SectionInfo>
          <Trans
            i18nKey="sidebar_description.edit_metadata_hint"
            ns="search"
            values={{ viewType: viewTypeLabel }}
            components={{ icon: <MoreActionsIcon /> }}
          />
        </SectionInfo>
      )}
      {resultsSection}
      <SectionSubheadline>{t('sidebar_description.search_section')}</SectionSubheadline>
      <p>{viewMetadata.summary || <i>{t('sidebar_description.no_summary', { viewType: viewTypeLabel })}</i>}</p>
      <p>{viewMetadata.description || <i>{t('sidebar_description.no_description', { viewType: viewTypeLabel })}</i>}</p>
    </>
  );
};

export default ViewDescription;
