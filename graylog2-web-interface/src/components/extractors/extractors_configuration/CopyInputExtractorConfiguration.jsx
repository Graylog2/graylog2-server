import React from 'react';
import { Panel } from 'react-bootstrap';

const CopyInputExtractorConfiguration = React.createClass({
  render() {
    return (
      <div className="form-group">
        <div className="col-md-offset-2 col-md-10">
          <Panel bsStyle="info" style={{ marginBottom: 0 }}>
            The entire input will be copied verbatim.
          </Panel>
        </div>
      </div>
    );
  },
});
export default CopyInputExtractorConfiguration;
