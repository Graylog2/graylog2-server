import PropTypes from 'prop-types';
import React from 'react';
import { Col, Row } from 'components/graylog';

import { MetricsFilterInput, MetricsList } from 'components/metrics';

class MetricsComponent extends React.Component {
  static propTypes = {
    names: PropTypes.arrayOf(PropTypes.object).isRequired,
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    filter: PropTypes.string,
  };

  static defaultProps = { filter: '' };

  state = { filter: this.props.filter };

  componentWillReceiveProps(nextProps) {
    if (nextProps.filter !== this.props.filter) {
      this.setState({ filter: nextProps.filter });
    }
  }

  onFilterChange = (nextFilter) => {
    this.setState({ filter: nextFilter });
  };

  render() {
    const { filter } = this.state;

    let filteredNames;
    try {
      const filterRegex = new RegExp(filter, 'i');
      filteredNames = this.props.names.filter((metric) => String(metric.full_name).match(filterRegex));
    } catch (e) {
      filteredNames = [];
    }
    return (
      <Row className="content">
        <Col md={12}>
          <MetricsFilterInput filter={filter} onChange={this.onFilterChange} />
          <MetricsList names={filteredNames} namespace={this.props.namespace} nodeId={this.props.nodeId} />
        </Col>
      </Row>
    );
  }
}

export default MetricsComponent;
