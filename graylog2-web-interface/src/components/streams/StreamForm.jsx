import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { IndexSetsActions } = CombinedProvider.get('IndexSets');

const StreamForm = React.createClass({
  propTypes: {
    onSubmit: React.PropTypes.func.isRequired,
    stream: React.PropTypes.object.isRequired,
    title: React.PropTypes.string.isRequired,
    indexSets: React.PropTypes.array.isRequired,
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
    let defaultIndexSetId = props.stream.index_set_id;
    if (!defaultIndexSetId && props.indexSets && props.indexSets.length > 0) {
      const defaultIndexSet = props.indexSets.find(indexSet => indexSet.default);
      if (defaultIndexSet) {
        defaultIndexSetId = defaultIndexSet.id;
      }
    }

    return {
      title: props.stream.title,
      description: props.stream.description,
      remove_matches_from_default_stream: props.stream.remove_matches_from_default_stream,
      index_set_id: defaultIndexSetId,
    };
  },

  _onSubmit() {
    this.props.onSubmit(this.props.stream.id,
      {
        title: this.state.title,
        description: this.state.description,
        remove_matches_from_default_stream: this.state.remove_matches_from_default_stream,
        index_set_id: this.state.index_set_id,
      });
    this.refs.modal.close();
  },

  open() {
    this._resetValues();
    IndexSetsActions.list(false);
    this.refs.modal.open();
  },

  close() {
    this.refs.modal.close();
  },

  _formatSelectOptions() {
    return this.props.indexSets.filter(indexSet => indexSet.writable).map((indexSet) => {
      return { value: indexSet.id, label: indexSet.title };
    });
  },

  _onIndexSetSelect(selection) {
    this.linkState('index_set_id').requestChange(selection);
  },

  render() {
    let indexSetSelect;
    if (this.props.indexSets) {
      indexSetSelect = (
        <div className="form-group">
          <label>Index Set</label>
          <Select placeholder="Select index set" options={this._formatSelectOptions()} matchProp="label"
                  onValueChange={this._onIndexSetSelect} value={this.state.index_set_id} />
          <p className="help-block">Messages that match this stream will be written to the configured index set.</p>
        </div>
      );
    } else {
      indexSetSelect = <Spinner>Loading index sets...</Spinner>;
    }

    return (
      <BootstrapModalForm ref="modal"
                          title={this.props.title}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save">
        <Input id="Title" type="text" required label="Title" name="Title"
               placeholder="A descriptive name of the new stream"
               valueLink={this.linkState('title')} autoFocus />
        <Input id="Description" type="text" required label="Description" name="Description"
               placeholder="What kind of messages are routed into this stream?"
               valueLink={this.linkState('description')} />
        {indexSetSelect}
        <Input id="RemoveFromDefaultStream" type="checkbox" label="Remove matches from 'All messages' stream" name="Remove from All messages"
               help={<span>Remove messages that match this stream from the 'All messages' stream which is assigned to every message by default.</span>}
               checkedLink={this.linkState('remove_matches_from_default_stream')} />
      </BootstrapModalForm>
    );
  },
});

export default StreamForm;
