import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { Input } from 'react-bootstrap';

const StreamForm = React.createClass({
  propTypes: {
    onSubmit: React.PropTypes.func.isRequired,
    stream: React.PropTypes.object.isRequired,
    title: React.PropTypes.string.isRequired,
  },

  mixins: [LinkedStateMixin],

  getDefaultProps() {
    return {
      stream: {
        title: '',
        description: '',
        remove_matches_from_default_stream: false,
      },
    };
  },

  getInitialState() {
    return this._getValuesFromProps(this.props);
  },

  _resetValues() {
    this.setState(this._getValuesFromProps(this.props));
  },

  _getValuesFromProps(props) {
    return {
      title: props.stream.title,
      description: props.stream.description,
      remove_matches_from_default_stream: props.stream.remove_matches_from_default_stream,
    };
  },

  _onSubmit() {
    this.props.onSubmit(this.props.stream.id,
      {
        title: this.state.title,
        description: this.state.description,
        remove_matches_from_default_stream: this.state.remove_matches_from_default_stream,
      });
    this.refs.modal.close();
  },

  open() {
    this._resetValues();
    this.refs.modal.open();
  },

  close() {
    this.refs.modal.close();
  },

  render() {
    return (
      <BootstrapModalForm ref="modal"
                          title={this.props.title}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save">
        <Input type="text" required label="Title" placeholder="A descriptive name of the new stream"
               valueLink={this.linkState('title')} autoFocus/>
        <Input type="text" required label="Description"
               placeholder="What kind of messages are routed into this stream?"
               valueLink={this.linkState('description')}/>
        <Input type="checkbox" label="Remove matches from 'All messages' stream"
               checkedLink={this.linkState('remove_matches_from_default_stream')}/>
      </BootstrapModalForm>
    );
  },
});

export default StreamForm;
