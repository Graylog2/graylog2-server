import PropTypes from 'prop-types';
import React from 'react';
import { Alert, Icon } from 'components/graylog';

class WidgetVisualizationNotFound extends React.Component {
  static propTypes = {
    widgetClassName: PropTypes.string.isRequired,
    onRenderComplete: PropTypes.func,
  };

  static defaultProps = {
    onRenderComplete: () => {},
  };

  componentDidMount() {
    this.props.onRenderComplete();
  }

  render() {
    return (
      <Alert bsStyle="danger">
        <Icon name="exclamation-circle" /> Widget Visualization (<i>{this.props.widgetClassName}</i>) not found.
        It looks like the plugin supplying this widget is not loaded.
      </Alert>
    );
  }
}

export default WidgetVisualizationNotFound;
