import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert, Col, Row } from 'components/graylog';
import { Icon } from 'components/common';
import { MetricsFilterInput, MetricsList } from 'components/metrics';

const StyledWarningDiv = styled.div(({ theme }) => `
  height: 20px;
  margin-bottom: 5px;
  color: ${theme.color.variant.dark.danger};
`);

class MetricsComponent extends React.Component {
  static propTypes = {
    names: PropTypes.arrayOf(PropTypes.object),
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    filter: PropTypes.string,
    error: PropTypes.shape({
      responseMessage: PropTypes.string,
      status: PropTypes.number,
    }),
  };

  static defaultProps = {
    names: undefined,
    filter: '',
    error: undefined,
  };

  state = { filter: this.props.filter };

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.filter !== this.props.filter) {
      this.setState({ filter: nextProps.filter });
    }
  }

  onFilterChange = (nextFilter) => {
    this.setState({ filter: nextFilter });
  };

  render() {
    const { filter } = this.state;
    const { names, error } = this.props;

    if (!names) {
      return (
        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="danger">
              <Icon name="exclamation-triangle" />&nbsp;
              There was a problem fetching node metrics. Graylog will keep trying to get them in the background.
            </Alert>
          </Col>
        </Row>
      );
    }

    let filteredNames;

    try {
      const filterRegex = new RegExp(filter, 'i');

      filteredNames = names.filter((metric) => String(metric.full_name).match(filterRegex));
    } catch (e) {
      filteredNames = [];
    }

    return (
      <Row className="content">
        <Col md={12}>
          <StyledWarningDiv className="text-warning">
            {error && (
              <>
                <Icon name="exclamation-triangle" />&nbsp;
                Fetching metrics from node returned {error.responseMessage || ''} with a {error.status} status code,{' '}
                displaying last metrics available.
              </>
            )}
          </StyledWarningDiv>
          <MetricsFilterInput filter={filter} onChange={this.onFilterChange} />
          <MetricsList names={filteredNames} namespace={this.props.namespace} nodeId={this.props.nodeId} />
        </Col>
      </Row>
    );
  }
}

export default MetricsComponent;
