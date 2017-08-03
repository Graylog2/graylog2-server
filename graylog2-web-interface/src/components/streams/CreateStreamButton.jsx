import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';
import StreamForm from 'components/streams/StreamForm';

const CreateStreamButton = React.createClass({
  propTypes: {
    buttonText: PropTypes.string,
    bsStyle: PropTypes.string,
    bsSize: PropTypes.string,
    className: PropTypes.string,
    onSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
  },
  getDefaultProps() {
    return {
      buttonText: 'Create Stream',
    };
  },
  onClick() {
    this.refs.streamForm.open();
  },
  render() {
    return (
      <span>
        <Button bsSize={this.props.bsSize} bsStyle={this.props.bsStyle} className={this.props.className}
                onClick={this.onClick}>
          {this.props.buttonText}
        </Button>
        <StreamForm ref="streamForm" title="Creating Stream" indexSets={this.props.indexSets}
                    onSubmit={this.props.onSave} />
      </span>
    );
  },
});

export default CreateStreamButton;
