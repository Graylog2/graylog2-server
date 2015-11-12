import React, {PropTypes} from 'react';
import {Input, Button} from 'react-bootstrap';

import ToolsStore from 'stores/tools/ToolsStore';
import UserNotification from 'util/UserNotification';

const SplitAndIndexExtractorConfiguration = React.createClass({
  propTypes: {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      trying: false,
    };
  },
  _onChange(key) {
    const onConfigurationChange = this.props.onChange(key);

    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      onConfigurationChange(event);
    };
  },
  _onTryClick() {
    this.setState({trying: true});

    const promise = ToolsStore.testSplitAndIndex(this.props.configuration.split_by, this.props.configuration.index,
      this.props.exampleMessage);

    promise.then(result => {
      if (!result.successful) {
        UserNotification.warning('We were not able to run the split and index extraction. Please check your parameters.');
        return;
      }

      const preview = (result.cut ? <samp>{result.cut}</samp> : '');
      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({trying: false}));
  },
  _isTryButtonDisabled() {
    const configuration = this.props.configuration;
    return this.state.trying || configuration.split_by === '' || configuration.index === undefined;
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
               defaultValue={this.props.configuration.split_by}
               onChange={this._onChange('split_by')}
               required
               help={splitByHelpMessage}/>

        <Input type="number"
               id="index"
               label="Target index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.index}
               onChange={this._onChange('index')}
               min="0"
               required
               help={indexHelpMessage}/>

        <Input wrapperClassName="col-md-offset-2 col-md-10">
          <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
            {this.state.trying ? <i className="fa fa-spin fa-spinner"/> : 'Try'}
          </Button>
        </Input>
      </div>
    );
  },
});

export default SplitAndIndexExtractorConfiguration;
