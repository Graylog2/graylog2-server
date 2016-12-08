import React from 'react';

import SourceOverview from 'components/sources/SourceOverview';
import { DocumentTitle } from 'components/common';

const SourcesPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="Sources">
        <SourceOverview />
      </DocumentTitle>
    );
  },
});

export default SourcesPage;
