import React, {PropTypes} from 'react';
import {Input, Button} from 'react-bootstrap';

import UserNotification from 'util/UserNotification';
import ToolsStore from 'stores/tools/ToolsStore';

const JSONExtractorConfiguration = React.createClass({
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

    const configuration = this.props.configuration;
    const promise = ToolsStore.testJSON(configuration.flatten, configuration.list_separator,
      configuration.key_separator, configuration.kv_separator, this.props.exampleMessage);

    promise.then(result => {
      const matches = [];
      for (const match in result.matches) {
        if ({}.hasOwnProperty.call(result.matches, match)) {
          matches.push(<dt key={`${match}-name`}>{match}</dt>);
          matches.push(<dd key={`${match}-value`}><samp>{result.matches[match]}</samp></dd>);
        }
      }

      this.props.onExtractorPreviewLoad(<dl>{matches}</dl>);
    });

    promise.finally(() => this.setState({trying: false}));
  },
  _isTryButtonDisabled() {
    const configuration = this.props.configuration;
    return this.state.trying || (configuration && (configuration.list_separator === '' || configuration.key_separator === '' || configuration.kv_separator === ''));
  },
  render() {
    return (
      <div>
        <Input type="checkbox"
               id="flatten"
               label="Flatten structures"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.props.configuration.flatten}
               onChange={this._onChange('flatten')}
               help="Whether to flatten JSON objects into a single message field or to expand into multiple fields."/>

        <Input type="text"
               id="list_separator"
               label="List item separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.list_separator}
               required
               onChange={this._onChange('list_separator')}
               help="What string to use to concatenate items of a JSON list."/>

        <Input type="text"
               id="key_separator"
               label="Key separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.key_separator}
               required
               onChange={this._onChange('key_separator')}
               help={<span>What string to use to concatenate different keys of a nested JSON object (only used if <em>not</em> flattened).</span>}/>

        <Input type="text"
               id="kv_separator"
               label="Key/value separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.kv_separator}
               required
               onChange={this._onChange('kv_separator')}
               help="What string to use when concatenating key/value pairs of a JSON object (only used if flattened)."/>

        <Input wrapperClassName="col-md-offset-2 col-md-10">
          <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
            {this.state.trying ? <i className="fa fa-spin fa-spinner"/> : 'Try'}
          </Button>
        </Input>
      </div>
    );
  },
});

export default JSONExtractorConfiguration;
