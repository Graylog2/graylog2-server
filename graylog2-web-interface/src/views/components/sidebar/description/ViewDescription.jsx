// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import QueryResult from 'views/logic/QueryResult';

import SearchResultOverview from './SearchResultOverview';

const Section = styled.div`
  margin-bottom: 8px;
`;

type Props = {
  results: QueryResult,
  viewMetadata: ViewMetaData,
};

const ViewDescription = ({ results, viewMetadata }: Props) => {
  const isAdHocSearch = !viewMetadata.id;
  const viewType = useContext(ViewTypeContext);
  const viewTypeLabel = viewType ? <ViewTypeLabel type={viewType} /> : '';
  const resultsSection = (
    <Section>
      <SearchResultOverview results={results} />
    </Section>
  );

  if (isAdHocSearch) {
    return (
      <>
        {resultsSection}
        <Section>
          <i>Save the search or export it to a dashboard to add a custom description.</i>
        </Section>
      </>
    );
  }

  return (
    <>
      {resultsSection}
      <Section>
        {viewMetadata.summary || <i>No {viewTypeLabel} summary.</i>}
      </Section>
      <Section>
        {viewMetadata.description || <i>No {viewTypeLabel} description.</i>}
      </Section>
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
