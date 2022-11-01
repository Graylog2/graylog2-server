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
import * as FormsUtils from 'util/FormsUtils';
import { IndexSetsActions } from 'stores/indices/IndexSetsStore';

const _getValuesFromProps = (props) => {
  let defaultIndexSetId = props.stream.index_set_id;

  if (!defaultIndexSetId && props.indexSets && props.indexSets.length > 0) {
    const defaultIndexSet = props.indexSets.find((indexSet) => indexSet.default);

    if (defaultIndexSet) {
      defaultIndexSetId = defaultIndexSet.id;
    }
  }

  return {
    showModal: false,
    title: props.stream.title,
    description: props.stream.description,
    removeMatchesFromDefaultStream: props.stream.remove_matches_from_default_stream,
    indexSetId: defaultIndexSetId,
  };
};

class StreamForm extends React.Component {
  static propTypes = {
    onSubmit: PropTypes.func.isRequired,
    stream: PropTypes.object,
    title: PropTypes.string.isRequired,
    submitButtonText: PropTypes.string.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  static defaultProps = {
    stream: {
      title: '',
      description: '',
      remove_matches_from_default_stream: false,
    },
  };

  constructor(props) {
    super(props);

    this.state = _getValuesFromProps(props);
  }

  _resetValues = () => {
    this.setState(_getValuesFromProps(this.props));
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  open = () => {
    this._resetValues();
    IndexSetsActions.list(false);
    this.setState({ showModal: true });
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  close = () => {
    this.setState({ showModal: false });
  };

  _onSubmit = () => {
    const { title, description, removeMatchesFromDefaultStream, indexSetId } = this.state;
    const { onSubmit, stream } = this.props;

    onSubmit(stream.id,
      {
        title,
        description,
        remove_matches_from_default_stream: removeMatchesFromDefaultStream,
        index_set_id: indexSetId,
      });

    this.close();
  };

  _formatSelectOptions = () => {
    const { indexSets } = this.props;

    return indexSets.filter((indexSet) => indexSet.can_be_default).map((indexSet) => {
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

  _indexSetSelect = () => {
    const { indexSetId } = this.state;
    const { indexSets } = this.props;

    if (indexSets) {
      return (
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
    }

    return <Spinner>Loading index sets...</Spinner>;
  };

  render() {
    const { title: propTitle, submitButtonText } = this.props;
    const { title, description, removeMatchesFromDefaultStream, showModal } = this.state;

    return (
      <BootstrapModalForm show={showModal}
                          title={propTitle}
                          onCancel={this.close}
                          onSubmitForm={this._onSubmit}
                          submitButtonText={submitButtonText}>
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
               label="Description"
               name="description"
               value={description}
               onChange={this.handleChange}
               placeholder="What kind of messages are routed into this stream?" />
        {this._indexSetSelect()}
        <Input id="RemoveFromDefaultStream"
               type="checkbox"
               label="Remove matches from &lsquo;Default Stream&rsquo;"
               name="removeMatchesFromDefaultStream"
               checked={removeMatchesFromDefaultStream}
               onChange={this.handleChange}
               help={<span>Don&apos;t assign messages that match this stream to the &lsquo;Default Stream&rsquo;.</span>} />
      </BootstrapModalForm>
    );
  }
}

export default StreamForm;
