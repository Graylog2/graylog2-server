import React from 'react';
import styled from 'styled-components';

import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import { SearchResultOverview } from 'views/components/sidebar';

const Section = styled.div`
  margin-bottom: 8px;
`;

const defaultNewViewSummary = 'No summary.';

type Props = {
  results: Object,
  viewMetadata: ViewMetaData,
};

const ViewDescription = ({ results, viewMetadata }: Props) => {
  const formatViewDescription = (view: ViewMetaData) => {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  };

  return (
    <React.Fragment>
      <Section>
        {viewMetadata.summary || defaultNewViewSummary}
      </Section>
      <Section>
        <SearchResultOverview results={results} />
      </Section>
      {formatViewDescription(viewMetadata)}
    </React.Fragment>
  );
};

export default ViewDescription;
