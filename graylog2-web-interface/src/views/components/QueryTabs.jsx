// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';

import { Tab, Tabs, Col, Row } from 'components/graylog';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import QueryTitle from 'views/components/queries/QueryTitle';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';

import { QueryIdsStore } from 'views/stores/QueryIdsStore';
import Query from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';

type Props = {
  children: React.Node,
  onRemove: (queryId: string) => Promise<void> | Promise<ViewState>,
  onSaveAsView: (view: View) => Promise<View>,
  onSaveView: (View) => void,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Array<QueryIdsStore>,
  selectedQueryId: string,
  titles: Immutable.Map<{[queryId: string]: string}>,
}

class QueryTabs extends React.Component<Props> {
  queryTitleEditModal: ?QueryTitleEditModal

  static propTypes = {
    children: PropTypes.node,
    onRemove: PropTypes.func.isRequired,
    onSaveAsView: PropTypes.func.isRequired,
    onSaveView: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    onTitleChange: PropTypes.func.isRequired,
    queries: PropTypes.object.isRequired,
    selectedQueryId: PropTypes.string.isRequired,
    titles: PropTypes.object.isRequired,
  }

  static defaultProps = {
    children: null,
  }

  openTitleEditModal = (activeQueryTitle: string) => {
    if (this.queryTitleEditModal) {
      this.queryTitleEditModal.open(activeQueryTitle);
    }
  }

  render() {
    const {
      children,
      onRemove,
      onSaveAsView,
      onSaveView,
      onSelect,
      onTitleChange,
      queries,
      selectedQueryId,
      titles,
    } = this.props;
    const queryTitles = titles;
    const queryTabs = queries.map((id, index) => {
      const title = queryTitles.get(id, `Query#${index + 1}`);
      const tabTitle = (
        <QueryTitle active={id === selectedQueryId}
                    id={id}
                    onClose={() => onRemove(id)}
                    openEditModal={this.openTitleEditModal}
                    title={title} />
      );
      return (
        <Tab eventKey={id}
             key={id}
             mountOnEnter
             title={tabTitle}
             unmountOnExit>
          {children}
        </Tab>
      );
    });
    const newTab = <Tab key="new" eventKey="new" title="+" />;
    const tabs = [queryTabs, newTab];

    return (
      <Row style={{ marginBottom: 0 }}>
        <Col>
          <span className="pull-right">
            <ViewActionsMenu onSaveView={onSaveView} onSaveAsView={onSaveAsView} />
          </span>
          <Tabs activeKey={selectedQueryId}
                animation={false}
                id="QueryTabs"
                onSelect={onSelect}>
            {tabs}
          </Tabs>
          {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
          <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(selectedQueryId, newTitle)}
                               ref={(queryTitleEditModal) => { this.queryTitleEditModal = queryTitleEditModal; }} />
        </Col>
      </Row>
    );
  }
}

export default QueryTabs;
