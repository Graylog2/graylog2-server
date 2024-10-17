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
import React from 'react';

import { ActionContext } from 'views/logic/ActionContext';
import ExtractorUtils from 'util/ExtractorUtils';
import Select from 'components/common/Select';
import { BootstrapModalForm } from 'components/bootstrap';
import type { ActionContexts } from 'views/types';

import type { ActionComponentProps } from '../../components/actions/ActionHandler';

type State = {
  selectedExtractor: string | undefined | null,
};

const _renderOption = ({ label }: { label: string }) => <strong>{label}</strong>;

const _getExtractorTypes = () => ExtractorUtils.EXTRACTOR_TYPES.map((extractorType) => ({ label: ExtractorUtils.getReadableExtractorTypeName(extractorType), value: extractorType }));

class SelectExtractorType extends React.Component<ActionComponentProps, State> {
  static contextType = ActionContext;

  readonly context: ActionContexts;

  extractorRoutes = {};

  constructor(props) {
    super(props);

    this.state = {
      selectedExtractor: undefined,
    };
  }

  componentDidMount() {
    const { message } = this.context;
    const { gl2_source_node, gl2_source_input } = message.fields;
    const { field } = this.props;

    this.extractorRoutes = ExtractorUtils.getNewExtractorRoutes(gl2_source_node,
      gl2_source_input,
      field,
      message.index,
      message.id);
  }

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

  _onChange = (selectedExtractor: string) => {
    this.setState({ selectedExtractor });
  };

  render() {
    const { selectedExtractor } = this.state;

    return (
      <BootstrapModalForm title="Select extractor type"
                          submitButtonDisabled={!selectedExtractor}
                          show
                          onCancel={this.props.onClose}
                          onSubmitForm={this._onSubmit}>
        <Select placeholder="Select extractor type"
                optionRenderer={_renderOption}
                clearable
                onChange={this._onChange}
                options={_getExtractorTypes()} />
      </BootstrapModalForm>
    );
  }
}

export default SelectExtractorType;
