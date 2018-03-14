import React from 'react';

import SourceOverview from 'components/sources/SourceOverview';
import { DocumentTitle } from 'components/common';

class SourcesPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="Sources">
        <SourceOverview />
      </DocumentTitle>
    );
  }
}

export default SourcesPage;
