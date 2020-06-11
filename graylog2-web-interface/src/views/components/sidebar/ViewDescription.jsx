// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import QueryResult from 'views/logic/QueryResult';

import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { SearchResultOverview } from 'views/components/sidebar';

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
      <Section>
        {viewMetadata.summary || <i>No {viewTypeLabel} summary.</i>}
      </Section>
      {resultsSection}
      <Section>
        {viewMetadata.description || <i>No {viewTypeLabel} description.</i>}
      </Section>
    </>
  );
};

ViewDescription.propTypes = {
  results: PropTypes.object.isRequired,
  viewMetadata: PropTypes.object.isRequired,
};

export default ViewDescription;
