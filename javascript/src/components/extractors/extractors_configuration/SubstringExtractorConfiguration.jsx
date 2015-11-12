import React, {PropTypes} from 'react';
import {Input, Button} from 'react-bootstrap';

import ToolsStore from 'stores/tools/ToolsStore';
import UserNotification from 'util/UserNotification';

const SubstringExtractorConfiguration = React.createClass({
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
  _verifySubstringInputs() {
    const beginIndex = this.refs.beginIndex.getInputDOMNode();
    const endIndex = this.refs.endIndex.getInputDOMNode();

    if (this.props.configuration.begin_index === undefined || this.props.configuration.begin_index < 0) {
      beginIndex.value = 0;
      this._onChange('begin_index')({target: beginIndex});
    }

    if (this.props.configuration.end_index === undefined || this.props.configuration.end_index < 0) {
      endIndex.value = 0;
      this._onChange('end_index')({target: endIndex});
    }

    if (this.props.configuration.begin_index > this.props.configuration.end_index) {
      beginIndex.value = this.props.configuration.end_index;
      this._onChange('begin_index')({target: beginIndex});
    }
  },
  _onTryClick() {
    this.setState({trying: true});

    this._verifySubstringInputs();

    if (this.state.configuration.begin_index === this.state.configuration.end_index) {
      this.props.onExtractorPreviewLoad('');
      this.setState({trying: false});
    } else {
      const promise = ToolsStore.testSubstring(this.props.configuration.begin_index, this.props.configuration.end_index,
        this.props.exampleMessage);

      promise.then(result => {
        if (!result.successful) {
          UserNotification.warning('We were not able to run the substring extraction. Please check index boundaries.');
          return;
        }
        this.props.onExtractorPreviewLoad(<samp>{result.cut}</samp>);
      });

      promise.finally(() => this.setState({trying: false}));
    }
  },
  _isTryButtonDisabled() {
    return this.state.trying || this.props.configuration.begin_index === undefined || this.props.configuration.end_index === undefined;
  },
  render() {
    const endIndexHelpMessage = (
      <span>
        Where to end extracting. (Exclusive){' '}
        <strong>Example:</strong> <em>1,5</em> cuts <em>love</em> from the string <em>ilovelogs</em>.
      </span>
    );
    return (
      <div>
        <Input type="number"
               ref="beginIndex"
               id="begin_index"
               label="Begin index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.begin_index}
               onChange={this._onChange('begin_index')}
               min="0"
               required
               help="Character position from where to start extracting. (Inclusive)"/>

        <Input type="number"
               ref="endIndex"
               id="end_index"
               label="End index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.end_index}
               onChange={this._onChange('end_index')}
               min="0"
               required
               help={endIndexHelpMessage}/>

        <Input wrapperClassName="col-md-offset-2 col-md-10">
          <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
            {this.state.trying ? <i className="fa fa-spin fa-spinner"/> : 'Try'}
          </Button>
        </Input>
      </div>
    );
  },
});

export default SubstringExtractorConfiguration;
