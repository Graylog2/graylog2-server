import React, { PropTypes } from 'react';
import { Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
const ToolsStore = StoreProvider.getStore('Tools');

import UserNotification from 'util/UserNotification';
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';

const SplitAndIndexExtractorConfiguration = React.createClass({
  propTypes: {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      trying: false,
      configuration: this._getEffectiveConfiguration(this.props.configuration),
    };
  },
  componentDidMount() {
    this.props.onChange(this.state.configuration);
  },
  componentWillReceiveProps(nextProps) {
    this.setState({ configuration: this._getEffectiveConfiguration(nextProps.configuration) });
  },
  DEFAULT_CONFIGURATION: { index: 1 },
  _getEffectiveConfiguration(configuration) {
    return ExtractorUtils.getEffectiveConfiguration(this.DEFAULT_CONFIGURATION, configuration);
  },
  _onChange(key) {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.state.configuration;
      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  },
  _onTryClick() {
    this.setState({ trying: true });

    const promise = ToolsStore.testSplitAndIndex(this.state.configuration.split_by, this.state.configuration.index,
      this.props.exampleMessage);

    promise.then((result) => {
      if (!result.successful) {
        UserNotification.warning('We were not able to run the split and index extraction. Please check your parameters.');
        return;
      }

      const preview = (result.cut ? <samp>{result.cut}</samp> : '');
      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  },
  _isTryButtonDisabled() {
    const configuration = this.state.configuration;
    return this.state.trying || configuration.split_by === '' || configuration.index === undefined || configuration.index < 1 || !this.props.exampleMessage;
  },
  render() {
    const splitByHelpMessage = (
      <span>
        What character to split on. <strong>Example:</strong> A whitespace character will split{' '}
        <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.
      </span>
    );

    const indexHelpMessage = (
      <span>
        What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em>{' '}
        from <em>foo bar baz</em> when split by whitespace.
      </span>
    );

    return (
      <div>
        <Input type="text"
               id="split_by"
               label="Split by"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.split_by}
               onChange={this._onChange('split_by')}
               required
               help={splitByHelpMessage} />

        <Input type="number"
               id="index"
               label="Target index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.index}
               onChange={this._onChange('index')}
               min="1"
               required
               help={indexHelpMessage} />

        <Input wrapperClassName="col-md-offset-2 col-md-10">
          <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
            {this.state.trying ? <i className="fa fa-spin fa-spinner" /> : 'Try'}
          </Button>
        </Input>
      </div>
    );
  },
});

export default SplitAndIndexExtractorConfiguration;
