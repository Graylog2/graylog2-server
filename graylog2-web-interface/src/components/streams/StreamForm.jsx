/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import * as FormsUtils from 'util/FormsUtils';

const { IndexSetsActions } = CombinedProvider.get('IndexSets');

class StreamForm extends React.Component {
  static propTypes = {
    onSubmit: PropTypes.func.isRequired,
    stream: PropTypes.object.isRequired,
    title: PropTypes.string.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  static defaultProps = {
    stream: {
      title: '',
      description: '',
      remove_matches_from_default_stream: false,
    },
  };

  modal = undefined;

  _resetValues = () => {
    this.setState(this._getValuesFromProps(this.props));
  };

  _getValuesFromProps = (props) => {
    let defaultIndexSetId = props.stream.index_set_id;

    if (!defaultIndexSetId && props.indexSets && props.indexSets.length > 0) {
      const defaultIndexSet = props.indexSets.find((indexSet) => indexSet.default);

      if (defaultIndexSet) {
        defaultIndexSetId = defaultIndexSet.id;
      }
    }

    return {
      title: props.stream.title,
      description: props.stream.description,
      removeMatchesFromDefaultStream: props.stream.remove_matches_from_default_stream,
      indexSetId: defaultIndexSetId,
    };
  };

  _onSubmit = () => {
    this.props.onSubmit(this.props.stream.id,
      {
        title: this.state.title,
        description: this.state.description,
        remove_matches_from_default_stream: this.state.removeMatchesFromDefaultStream,
        index_set_id: this.state.indexSetId,
      });

    this.modal.close();
  };

  open = () => {
    this._resetValues();
    IndexSetsActions.list(false);
    this.modal.open();
  };

  close = () => {
    this.modal.close();
  };

  _formatSelectOptions = () => {
    return this.props.indexSets.filter((indexSet) => indexSet.writable).map((indexSet) => {
      return { value: indexSet.id, label: indexSet.title };
    });
  };

  _onIndexSetSelect = (selection) => {
    this.setState({ indexSetId: selection });
  };

  handleChange = (event) => {
    const change = {};

    change[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState(change);
  };

  state = this._getValuesFromProps(this.props);

  render() {
    const { title, description, removeMatchesFromDefaultStream, indexSetId } = this.state;

    let indexSetSelect;

    if (this.props.indexSets) {
      indexSetSelect = (
        <Input id="index-set-selector"
               label="Index Set"
               help="Messages that match this stream will be written to the configured index set.">
          <Select inputId="index-set-selector"
                  placeholder="Select index set"
                  options={this._formatSelectOptions()}
                  matchProp="label"
                  onChange={this._onIndexSetSelect}
                  value={indexSetId} />
        </Input>
      );
    } else {
      indexSetSelect = <Spinner>Loading index sets...</Spinner>;
    }

    return (
      <BootstrapModalForm ref={(c) => { this.modal = c; }}
                          title={this.props.title}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save">
        <Input id="Title"
               type="text"
               required
               label="Title"
               name="title"
               value={title}
               onChange={this.handleChange}
               placeholder="A descriptive name of the new stream"
               autoFocus />
        <Input id="Description"
               type="text"
               required
               label="Description"
               name="description"
               value={description}
               onChange={this.handleChange}
               placeholder="What kind of messages are routed into this stream?" />
        {indexSetSelect}
        <Input id="RemoveFromDefaultStream"
               type="checkbox"
               label="Remove matches from &lsquo;All messages&rsquo; stream"
               name="removeMatchesFromDefaultStream"
               checked={removeMatchesFromDefaultStream}
               onChange={this.handleChange}
               help={<span>Remove messages that match this stream from the &lsquo;All messages&rsquo; stream which is assigned to every message by default.</span>} />
      </BootstrapModalForm>
    );
  }
}

export default StreamForm;
