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
// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { ActionContext } from 'views/logic/ActionContext';
import ExtractorUtils from 'util/ExtractorUtils';
import Select from 'components/common/Select';
import { BootstrapModalForm } from 'components/bootstrap';

import type { ActionComponentProps } from '../../components/actions/ActionHandler';

type State = {
  selectedExtractor: ?string,
};

class SelectExtractorType extends React.Component<ActionComponentProps, State> {
  static propTypes = {
    onClose: PropTypes.func.isRequired,
  };

  static contextType = ActionContext;

  state = {
    selectedExtractor: undefined,
  };

  extractorRoutes = {};

  componentDidMount() {
    const { message } = this.context;
    // eslint-disable-next-line camelcase
    const { gl2_source_node, gl2_source_input } = message.fields;
    const { field } = this.props;

    this.extractorRoutes = ExtractorUtils.getNewExtractorRoutes(gl2_source_node,
      gl2_source_input, field, message.index, message.id);
  }

  /* eslint-disable-next-line react/no-unused-prop-types */
  _renderOption = ({ label }: { label: string }) => <><strong>{label}</strong></>;

  _onSubmit = () => {
    const { onClose } = this.props;

    onClose();

    const { selectedExtractor } = this.state;

    if (selectedExtractor) {
      const uri = this.extractorRoutes[selectedExtractor];
      const newWindow = window.open(uri, '_blank');

      newWindow.focus();
    }
  };

  _getExtractorTypes = () => {
    return ExtractorUtils.EXTRACTOR_TYPES.map((extractorType) => {
      return { label: ExtractorUtils.getReadableExtractorTypeName(extractorType), value: extractorType };
    });
  };

  _onChange = (selectedExtractor: string) => {
    this.setState({ selectedExtractor });
  };

  render() {
    const { selectedExtractor } = this.state;

    return (
      <BootstrapModalForm title="Select extractor type"
                          submitButtonDisabled={!selectedExtractor}
                          show
                          onSubmitForm={this._onSubmit}>
        <Select placeholder="Select extractor type"
                optionRenderer={this._renderOption}
                clearable
                onChange={this._onChange}
                options={this._getExtractorTypes()} />
      </BootstrapModalForm>
    );
  }
}

export default SelectExtractorType;
