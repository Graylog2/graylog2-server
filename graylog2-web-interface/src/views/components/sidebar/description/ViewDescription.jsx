// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import QueryResult from 'views/logic/QueryResult';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { Icon } from 'components/common';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

import SearchResultOverview from './SearchResultOverview';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

type Props = {
  results: QueryResult,
  viewMetadata: ViewMetaData,
};

const ViewDescription = ({ results, viewMetadata }: Props) => {
  const isAdHocSearch = !viewMetadata.id;
  const viewType = useContext(ViewTypeContext);
  const viewTypeLabel = viewType ? <ViewTypeLabel type={viewType} /> : '';
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
        {viewMetadata.summary || <>This {viewTypeLabel} has no summary.</>}
      </p>
      <p>
        {viewMetadata.description || <>This {viewTypeLabel} has no description.</>}
      </p>
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
