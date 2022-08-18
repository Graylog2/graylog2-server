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
import lodash from 'lodash';
import React from 'react';

import LookupTableParameterEdit from 'components/lookup-table-parameters/LookupTableParameterEdit';
import { Button, BootstrapModalForm } from 'components/bootstrap';
import type { LookupTable } from 'logic/lookup-tables/types';
import type { LookupTableParameterJson } from 'views/logic/parameters/LookupTableParameter';
import type LookupTableParameter from 'views/logic/parameters/LookupTableParameter';

type Props = {
  queryParameters: Array<LookupTableParameterJson>,
  lookupTables: Array<LookupTable>,
  onChange: (newQueryParameters: Array<LookupTableParameterJson>) => void,
  queryParameter: LookupTableParameter,
  embryonic: boolean,
}

type State = {
  queryParameter: LookupTableParameter,
  validation: {
    lookupTable?: string,
    key?: string,
  },
}

class EditQueryParameterModal extends React.Component<Props, State> {
  static propTypes = {
    queryParameters: PropTypes.array.isRequired,
    queryParameter: PropTypes.object.isRequired,
    lookupTables: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    embryonic: PropTypes.bool.isRequired,
  };

  modal: BootstrapModalForm = React.createRef();

  constructor(props) {
    super(props);

    const { queryParameter } = this.props;

    this.state = {
      queryParameter,
      validation: {},
    };
  }

  openModal = () => {
    this.modal.open();
  };

  _saved = () => {
    const { queryParameter } = this.state;

    if (!this._validate(queryParameter)) {
      return;
    }

    this.propagateChanges();
    this.modal.close();
  };

  _cleanState = () => {
    const { queryParameter } = this.props;

    this.setState({ queryParameter });
  };

  propagateChanges = () => {
    const { queryParameters, onChange, queryParameter: prevQueryParameter } = this.props;
    const { queryParameter } = this.state;
    const newQueryParameters = [...queryParameters];
    const index = queryParameters.findIndex((p) => p.name === prevQueryParameter.name);

    if (index < 0) {
      throw new Error(`Query parameter "${queryParameter.name}" not found`);
    }

    newQueryParameters[index] = queryParameter.toJSON();
    onChange(newQueryParameters);
  };

  handleParameterChange = (key, value) => {
    const { queryParameter } = this.state;
    const nextQueryParameter = queryParameter.toBuilder()[key](value).build();

    this.setState({ queryParameter: nextQueryParameter });
  };

  _validate = (queryParameter) => {
    const newValidation: State['validation'] = {};

    if (!queryParameter.lookupTable) {
      newValidation.lookupTable = 'Cannot be empty';
    }

    if (!queryParameter.key) {
      newValidation.key = 'Cannot be empty';
    }

    this.setState({ validation: newValidation });

    return lodash.isEmpty(newValidation);
  };

  render() {
    const { lookupTables, embryonic } = this.props;
    const { queryParameter, validation } = this.state;

    const validationState: {
      lookupTable?: [string, string],
      key?: [string, string],
    } = {
      lookupTable: validation.lookupTable ? ['error', validation.lookupTable] : undefined,
      key: validation.key ? ['error', validation.key] : undefined,
    };

    return (
      <>
        <Button bsSize="small"
                bsStyle={embryonic ? 'primary' : 'info'}
                onClick={() => this.openModal()}>
          {queryParameter.name}{embryonic && ': undeclared'}
        </Button>

        <BootstrapModalForm ref={(ref) => { this.modal = ref; }}
                            title={`Declare Query Parameter "${queryParameter.name}" from Lookup Table`}
                            onSubmitForm={this._saved}
                            onModalClose={this._cleanState}
                            submitButtonText="Save">
          <LookupTableParameterEdit validationState={validationState}
                                    identifier={queryParameter.name}
                                    parameter={queryParameter}
                                    onChange={this.handleParameterChange}
                                    lookupTables={lookupTables} />
        </BootstrapModalForm>
      </>
    );
  }
}

export default EditQueryParameterModal;
