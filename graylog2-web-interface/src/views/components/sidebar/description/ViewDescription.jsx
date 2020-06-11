// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Icon } from 'components/common';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import QueryResult from 'views/logic/QueryResult';
import SectionInfo from '../SectionInfo';

import SearchResultOverview from './SearchResultOverview';

type Props = {
  results: QueryResult,
  viewMetadata: ViewMetaData,
};

const ViewDescription = ({ results, viewMetadata }: Props) => {
  const isAdHocSearch = !viewMetadata.id;
  const viewType = useContext(ViewTypeContext);
  const viewTypeLabel = viewType ? <ViewTypeLabel type={viewType} /> : '';
  const resultsSection = (
    <p>
      <SearchResultOverview results={results} />
    </p>
  );

  if (isAdHocSearch) {
    return (
      <>
        {resultsSection}
        <SectionInfo>Save the search or export it to a dashboard to add a custom summary and description.</SectionInfo>
      </>
    );
  }

  return (
    <>
      {resultsSection}
      <p>
        {viewMetadata.summary || <>This {viewTypeLabel} has no summary.</>}
      </p>
      <p>
        {viewMetadata.description || <>This {viewTypeLabel} has no description.</>}
      </p>

      {(!viewMetadata.summary || !viewMetadata.description) && (
        <SectionInfo>
          To add a description and summary for this {viewTypeLabel} click on the <Icon name="ellipsis-h" /> icon in the search bar to open its action menu. The action menu includes the option &quot;Edit metadata&quot;.
        </SectionInfo>
      )}
    </>
  );
};

ViewDescription.propTypes = {
  results: PropTypes.object.isRequired,
  viewMetadata: PropTypes.shape({
    activeQuery: PropTypes.string,
    description: PropTypes.string,
    id: PropTypes.string,
    summary: PropTypes.string,
    title: PropTypes.string,
  }).isRequired,
};

export default ViewDescription;
