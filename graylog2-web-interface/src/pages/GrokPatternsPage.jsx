import React from 'react';

import GrokPatterns from 'components/grok-patterns/GrokPatterns';
import { DocumentTitle } from 'components/common';

class GrokPatternsPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="Grok patterns">
        <GrokPatterns />
      </DocumentTitle>
    );
  }
}

export default GrokPatternsPage;
