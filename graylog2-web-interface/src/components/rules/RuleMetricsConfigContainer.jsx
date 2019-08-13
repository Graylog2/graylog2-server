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

  state = {
    showConfig: false,
  };

  componentDidMount() {
    RulesActions.loadMetricsConfig().then(this.showConfig).catch(this.showConfig);
  }

  showConfig = () => {
    this.setState({ showConfig: true });
  };

  handleClose = () => {
    const { onClose } = this.props;
    this.setState({ showConfig: false }, onClose);
  };

  handleChange = (nextConfig) => {
    return RulesActions.updateMetricsConfig(nextConfig);
  };

  render() {
    const { metricsConfig } = this.props;
    const { showConfig } = this.state;

    if (!showConfig) {
      return null;
    }

    return (
      <RuleMetricsConfig ref={(component) => { this.configComponent = component; }}
                         config={metricsConfig}
                         onChange={this.handleChange}
                         onClose={this.handleClose} />
    );
  }
}

export default connect(RuleMetricsConfigContainer, { rules: RulesStore }, ({ rules }) => {
  return { metricsConfig: rules ? rules.metricsConfig : rules };
});
