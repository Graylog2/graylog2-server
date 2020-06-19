import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import RuleMetricsConfig from './RuleMetricsConfig';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');

class RuleMetricsConfigContainer extends React.Component {
  static propTypes = {
    metricsConfig: PropTypes.object,
    onClose: PropTypes.func,
  };

  static defaultProps = {
    metricsConfig: undefined,
    onClose: () => {},
  };

  componentDidMount() {
    RulesActions.loadMetricsConfig();
  }

  handleChange = (nextConfig) => {
    return RulesActions.updateMetricsConfig(nextConfig);
  };

  render() {
    const { metricsConfig, onClose } = this.props;

    if (!metricsConfig) {
      return null;
    }

    return (
      <RuleMetricsConfig ref={(component) => { this.configComponent = component; }}
                         config={metricsConfig}
                         onChange={this.handleChange}
                         onClose={onClose} />
    );
  }
}

export default connect(RuleMetricsConfigContainer, { rules: RulesStore }, ({ rules }) => {
  return { metricsConfig: rules ? rules.metricsConfig : rules };
});
