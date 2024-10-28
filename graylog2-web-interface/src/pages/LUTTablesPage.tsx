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

import { LinkContainer } from 'components/common/router';
import connect from 'stores/connect';
import Routes from 'routing/Routes';
import { Col, Row, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import type { PaginationProps } from 'components/common/withPaginationQueryParameter';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { LookupTableView, LookupTableCreate, LookupTableForm, LookupTablesOverview } from 'components/lookup-tables';
import type { ParamsContext } from 'routing/withParams';
import withParams from 'routing/withParams';
import type { LocationContext } from 'routing/withLocation';
import withLocation from 'routing/withLocation';
import { LookupTablesActions, LookupTablesStore } from 'stores/lookup-tables/LookupTablesStore';
import LUTPageNavigation from 'components/lookup-tables/LUTPageNavigation';
import type { HistoryContext } from 'routing/withHistory';
import withHistory from 'routing/withHistory';

const _saved = (history) => {
  // reset detail state
  history.push(Routes.SYSTEM.LOOKUPTABLES.OVERVIEW);
};

const _isCreating = ({ action }: LUTTablesPageProps) => action === 'create';

const _validateTable = (table) => {
  LookupTablesActions.validate(table);
};

type LUTTablesPageProps = HistoryContext & PaginationProps & ParamsContext & LocationContext & {
  table?: any;
  validationErrors?: any;
  dataAdapter?: any;
  cache?: any;
  tables?: any[];
  caches?: any;
  dataAdapters?: { [key: string]: { name: string } };
  pagination?: any;
  location?: any;
  errorStates?: any;
  action?: string;
};

class LUTTablesPage extends React.Component<LUTTablesPageProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    errorStates: null,
    validationErrors: {},
    dataAdapters: null,
    table: null,
    cache: null,
    caches: null,
    tables: null,
    location: null,
    pagination: null,
    dataAdapter: null,
    action: undefined,
  };

  errorStatesTimer = undefined;

  errorStatesInterval = 1000;

  componentDidMount() {
    this._loadData(this.props);
  }

  componentDidUpdate(prevProps) {
    const { location: { pathname } } = this.props;
    const { location: { pathname: prevPathname } } = prevProps;

    if (pathname !== prevPathname) {
      this._loadData(this.props);
    }
  }

  componentWillUnmount() {
    const { page, pageSize } = this.props.paginationQueryParameter;

    clearInterval(this.errorStatesTimer);
    LookupTablesActions.searchPaginated(page, pageSize);
  }

  _startErrorStatesTimer = () => {
    const { tables, dataAdapters } = this.props;

    this._stopErrorStatesTimer();

    this.errorStatesTimer = setInterval(() => {
      let tableNames = null;

      if (tables) {
        tableNames = tables.map((t) => t.name);
      }

      if (tableNames) {
        const adapterNames = Object.values(dataAdapters).map((a) => a.name);

        LookupTablesActions.getErrors(tableNames, null, adapterNames || null);
      }
    }, this.errorStatesInterval);
  };

  _stopErrorStatesTimer = () => {
    if (this.errorStatesTimer) {
      clearInterval(this.errorStatesTimer);
      this.errorStatesTimer = undefined;
    }
  };

  _loadData = (props) => {
    const { pagination } = this.props;
    const { page, pageSize } = this.props.paginationQueryParameter;

    this._stopErrorStatesTimer();

    if (props.params && props.params.tableName) {
      LookupTablesActions.get(props.params.tableName);
    } else if (_isCreating(props)) {
      // nothing to do, the intermediate data container will take care of loading the caches and adapters
    } else {
      LookupTablesActions.searchPaginated(page, pageSize, pagination.query);
      this._startErrorStatesTimer();
    }
  };

  render() {
    const {
      action,
      table,
      validationErrors,
      dataAdapter,
      cache,
      tables,
      caches,
      dataAdapters,
      pagination,
      errorStates,
      history,
    } = this.props;
    let content;
    const isShowing = action === 'show';
    const isEditing = action === 'edit';
    const saved = () => _saved(history);

    if (isShowing || isEditing) {
      if (!table) {
        content = <Spinner text="Loading lookup table" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={8}>
              <h2>Lookup Table</h2>
              <LookupTableForm table={table}
                               create={false}
                               saved={saved}
                               validate={_validateTable}
                               validationErrors={validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = (
          <LookupTableView dataAdapter={dataAdapter}
                           cache={cache}
                           table={table} />
        );
      }
    } else if (_isCreating(this.props)) {
      content = (
        <LookupTableCreate saved={saved}
                           validate={_validateTable}
                           validationErrors={validationErrors} />
      );
    } else if (!tables) {
      content = <Spinner text="Loading lookup tables" />;
    } else {
      content = (
        <LookupTablesOverview tables={tables}
                              caches={caches}
                              dataAdapters={dataAdapters}
                              pagination={pagination}
                              errorStates={errorStates} />
      );
    }

    return (
      <DocumentTitle title="Lookup Tables">
        <LUTPageNavigation />
        <PageHeader title="Lookup Tables"
                    actions={(
                      <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CREATE}>
                        <Button bsStyle="success" style={{ marginLeft: 5 }}>Create lookup table</Button>
                      </LinkContainer>
                      )}>
          <span>Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages.</span>
        </PageHeader>

        {content}
      </DocumentTitle>
    );
  }
}

export default connect(withHistory(withParams(withLocation(withPaginationQueryParameter(LUTTablesPage)))), { lookupTableStore: LookupTablesStore }, ({ lookupTableStore, ...otherProps }) => ({
  ...otherProps,
  ...lookupTableStore,
}));
