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
import PropTypes from 'prop-types';

import type QueryResult from 'views/logic/QueryResult';
import { Icon } from 'components/common';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import useViewType from 'views/hooks/useViewType';
import useViewMetadata from 'views/hooks/useViewMetadata';

import SearchResultOverview from './SearchResultOverview';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

type Props = {
  results: QueryResult,
};

const ViewDescription = ({ results }: Props) => {
  const viewMetadata = useViewMetadata();
  const isAdHocSearch = !viewMetadata.id;
  const viewType = useViewType();
  const viewTypeLabel = viewType ? ViewTypeLabel({ type: viewType }) : '';
  const resultsSection = (
    <>
      <SectionSubheadline>
        Execution
      </SectionSubheadline>
      <p>

        <SearchResultOverview results={results} />
      </p>
    </>
  );

  if (isAdHocSearch) {
    return (
      <>
        <SectionInfo>Save the search or export it to a dashboard to add a custom summary and description.</SectionInfo>
        {resultsSection}
      </>
    );
  }

  return (
    <>
      {(!viewMetadata.summary || !viewMetadata.description) && (
        <SectionInfo>
          To add a description and summary for this {viewTypeLabel} click on the <Icon name="ellipsis-h" /> icon in the search bar to open its action menu. The action menu includes the option &quot;Edit metadata&quot;.
        </SectionInfo>
      )}
      {resultsSection}
      <SectionSubheadline>
        Search
      </SectionSubheadline>
      <p>
        {viewMetadata.summary || <i>This {viewTypeLabel} has no summary.</i>}
      </p>
      <p>
        {viewMetadata.description || <i>This {viewTypeLabel} has no description.</i>}
      </p>
    </>
  );
};

ViewDescription.propTypes = {
  results: PropTypes.object.isRequired,
};

export default ViewDescription;
