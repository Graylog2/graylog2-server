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
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Link, Select, Spinner, Icon } from 'components/common';
import { Row, Col, Button, Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import { testLookupTable } from 'api/tools';
import { fetchAllLookupTables } from 'components/lookup-tables/hooks/api/lookupTablesAPI';

type Props = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
} & WithTranslation;

class LookupTableExtractorConfiguration extends React.Component<
  Props,
  {
    trying: boolean;
    lookupTables: any;
  }
> {
  static defaultProps = {
    exampleMessage: '',
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      trying: false,
      lookupTables: undefined,
    };
  }

  componentDidMount() {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    fetchAllLookupTables().then((result) => {
      this.setState({ lookupTables: result });
    });
  }

  _updateConfigValue = (key, value) => {
    this.props.onExtractorPreviewLoad(undefined);
    const newConfig = this.props.configuration;

    newConfig[key] = value;
    this.props.onChange(newConfig);
  };

  _onSelect = (key) => (value) => this._updateConfigValue(key, value);

  _onTryClick = () => {
    this.setState({ trying: true });

    const promise = testLookupTable(this.props.configuration.lookup_table_name, this.props.exampleMessage);

    promise.then((result) => {
      if (result.error) {
        UserNotification.warning(this.props.t('lookup_table.error', { error: result.error_message }));

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

  _isTryButtonDisabled = () =>
    this.state.trying || !this.props.configuration.lookup_table_name || !this.props.exampleMessage;

  render() {
    const { t } = this.props;

    if (!this.state.lookupTables) {
      return <Spinner />;
    }

    const lookupTables = this.state.lookupTables.map((table) => ({ label: table.title, value: table.name }));

    const helpMessage = (
      <span>
        <Trans
          t={t}
          i18nKey="converters.lookup_table_help"
          components={{ lookupTablesLink: <Link to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} /> }}
        />
      </span>
    );

    return (
      <div>
        <Input
          id="lookup_table_name"
          label={t('converters.lookup_table')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <Select
                placeholder={t('converters.lookup_table_placeholder')}
                clearable={false}
                options={lookupTables}
                onChange={this._onSelect('lookup_table_name')}
                value={this.props.configuration.lookup_table_name}
              />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <Icon name="progress_activity" spin /> : t('edit.try')}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  }
}

export default withTranslation('extractors')(LookupTableExtractorConfiguration);
