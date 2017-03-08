import React from 'react';
import { Alert } from 'react-bootstrap';

const WidgetVisualizationNotFound = React.createClass({
  propTypes: {
    widgetClassName: React.PropTypes.string.isRequired,
  },
  render() {
    return (
      <Alert bsStyle="danger">
        <i className="fa fa-exclamation-circle" /> Widget Visualization (<i>{this.props.widgetClassName}</i>) not found.

        Seems like the plugin supplying this widget is not loaded.
      </Alert>
    );
  },
});

export default WidgetVisualizationNotFound;
