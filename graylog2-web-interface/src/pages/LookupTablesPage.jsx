import React, { PropTypes } from 'react';

import { DocumentTitle } from 'components/common';


const LookupTablesPage = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },

  render() {
    return (
      <DocumentTitle title="Lookup Tables">
        {this.props.children}
      </DocumentTitle>
    );
  },
});

export default LookupTablesPage;
