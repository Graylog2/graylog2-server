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

import { Link } from 'components/common/router';
import { Select, Spinner, Icon } from 'components/common';
import { Row, Col, Button, Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import FormUtils from 'util/FormsUtils';
import ToolsStore from 'stores/tools/ToolsStore';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';

type LookupTableExtractorConfigurationProps = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
};

class LookupTableExtractorConfiguration extends React.Component<LookupTableExtractorConfigurationProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    exampleMessage: '',
  };

  state = {
    trying: false,
    lookupTables: undefined,
  };

  componentDidMount() {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    LookupTablesActions.searchPaginated(1, 10000, null).then((result) => {
      this.setState({ lookupTables: result.lookup_tables });
    });
  }

  _updateConfigValue = (key, value) => {
    this.props.onExtractorPreviewLoad(undefined);
    const newConfig = this.props.configuration;

    newConfig[key] = value;
    this.props.onChange(newConfig);
  };

  _onChange = (key) => (event) => this._updateConfigValue(key, FormUtils.getValueFromInput(event.target));

  _onSelect = (key) => (value) => this._updateConfigValue(key, value);

  _onTryClick = () => {
    this.setState({ trying: true });

    const promise = ToolsStore.testLookupTable(this.props.configuration.lookup_table_name, this.props.exampleMessage);

    promise.then((result) => {
      if (result.error) {
        UserNotification.warning(`We were not able to run the lookup: ${result.error_message}`);

        return;
      }

      if (!result.empty) {
        this.props.onExtractorPreviewLoad(result.value);
      } else {
        this.props.onExtractorPreviewLoad(`no lookup result for "${result.key}"`);
      }
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _isTryButtonDisabled = () => this.state.trying || !this.props.configuration.lookup_table_name || !this.props.exampleMessage;

  render() {
    if (!this.state.lookupTables) {
      return <Spinner />;
    }

    const lookupTables = this.state.lookupTables.map((table) => ({ label: table.title, value: table.name }));

    const helpMessage = (
      <span>
        Lookup tables can be created <Link to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>here</Link>.
      </span>
    );

    return (
      <div>
        <Input id="lookup_table_name"
               label="Lookup Table"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <Select placeholder="Select a lookup table"
                      clearable={false}
                      options={lookupTables}
                      matchProp="label"
                      onChange={this._onSelect('lookup_table_name')}
                      value={this.props.configuration.lookup_table_name} />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <Icon name="progress_activity" spin /> : 'Try'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  }
}

export default LookupTableExtractorConfiguration;
