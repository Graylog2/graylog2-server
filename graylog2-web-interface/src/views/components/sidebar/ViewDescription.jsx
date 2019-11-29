// @flow strict
import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import IfDashboard from 'views/components/dashboard/IfDashboard';
import { SearchResultOverview } from 'views/components/sidebar';

const Section = styled.div`
  margin-bottom: 8px;
`;

const defaultNewDashboardSummary = 'No dashboard summary.';

type Props = {
  results: Object,
  viewMetadata: ViewMetaData,
};

const ViewDescription = ({ results, viewMetadata }: Props) => {
  const formatDashboardDescription = (view: ViewMetaData) => {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No dashboard description.</i>;
  };

  return (
    <React.Fragment>
      <IfDashboard>
        <Section>
          {viewMetadata.summary || defaultNewDashboardSummary}
        </Section>
      </IfDashboard>
      <Section>
        <SearchResultOverview results={results} />
      </Section>
      <IfDashboard>
        {formatDashboardDescription(viewMetadata)}
      </IfDashboard>
    </React.Fragment>
  );
};

ViewDescription.propTypes = {
  results: PropTypes.object.isRequired,
  viewMetadata: PropTypes.object.isRequired,
};

export default ViewDescription;
