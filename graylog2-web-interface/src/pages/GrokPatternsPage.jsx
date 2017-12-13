import React from 'react';

import GrokPatterns from 'components/grok-patterns/GrokPatterns';
import { DocumentTitle, IfPermitted } from 'components/common';

const GrokPatternsPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="Grok patterns">
        <IfPermitted permissions="inputs:read">
          <GrokPatterns />
        </IfPermitted>
      </DocumentTitle>
    );
  },
});

export default GrokPatternsPage;
