// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { isEmpty, get } from 'lodash';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { Messages } from 'views/Constants';
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchActions, SearchStore } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { PaginatedList } from 'components/common';
import CustomPropTypes from 'views/components/CustomPropTypes';
import MessageTable from 'views/components/widgets/MessageTable';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import RenderCompletionCallback from './RenderCompletionCallback';

const { InputsActions } = CombinedProvider.get('Inputs');

const Wrapper = styled.div`
  display: grid;
  display: -ms-grid;
  -ms-grid-row: 2;
  -ms-grid-column: 1;
  grid-template-rows: 1fr auto;
  -ms-grid-rows: 1fr auto;
  -ms-grid-columns: 1fr;
  height: 100%;

  .pagination-wrapper {
    grid-row: 2;
    -ms-grid-row: 2;
    grid-column: 1;
    -ms-grid-column: 1;
  }

  .pagination {
    margin-bottom: 0;
  }
`;

type State = {
  errors: Array<{ description: string }>,
  currentPage: number,
};

type Props = {
  config: MessagesWidgetConfig,
  currentView: ViewStoreState,
  data: { messages: Array<Object>, total: number, id: string },
  editing: boolean,
  fields: FieldTypeMappingsList,
  onConfigChange: (MessagesWidgetConfig) => Promise<void>,
  pageSize: number,
  searchTypes: { [searchTypeId: string]: { effectiveTimerange: TimeRange }},
  selectedFields?: Immutable.Set<string>,
  setLoadingState: (loading: boolean) => void,
};

class MessageList extends React.Component<Props, State> {
  static propTypes = {
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
    currentView: PropTypes.object.isRequired,
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
      total: PropTypes.number.isRequired,
      id: PropTypes.string.isRequired,
    }).isRequired,
    editing: PropTypes.bool.isRequired,
    fields: CustomPropTypes.FieldListType.isRequired,
    onConfigChange: PropTypes.func,
    pageSize: PropTypes.number,
    searchTypes: PropTypes.object.isRequired,
    selectedFields: PropTypes.object,
    setLoadingState: PropTypes.func.isRequired,
  };

  static defaultProps = {
    onConfigChange: () => Promise.resolve(),
    pageSize: Messages.DEFAULT_LIMIT,
    selectedFields: Immutable.Set(),
  };

  state = {
    errors: [],
    currentPage: 1,
  };

  componentDidMount() {
    const onRenderComplete = this.context;

    InputsActions.list().then(() => (onRenderComplete && onRenderComplete()));
    SearchActions.execute.completed.listen(this._resetPagination);
  }

  _resetPagination = () => {
    const { currentPage } = this.state;

    if (currentPage !== 1) {
      this.setState({ currentPage: 1, errors: [] });
    }
  };

  _handlePageChange = (pageNo: number) => {
    // execute search with new offset
    const { pageSize, searchTypes, data: { id: searchTypeId }, setLoadingState } = this.props;
    const { effectiveTimerange } = searchTypes[searchTypeId];
    const searchTypePayload = { [searchTypeId]: { limit: pageSize, offset: pageSize * (pageNo - 1) } };

    RefreshActions.disable();
    setLoadingState(true);
    SearchActions.reexecuteSearchTypes(searchTypePayload, effectiveTimerange).then((response) => {
      setLoadingState(false);
      this.setState({
        errors: response.result.errors,
        currentPage: pageNo,
      });
    });
  };

  _getListKey = () => {
    // When the component receives new messages, we want to reset the scroll position, by defining a new key or the MessageTable.
    const { data: { messages } } = this.props;
    const { currentPage } = this.state;
    const defaultKey = `message-list-${currentPage}`;

    if (!isEmpty(messages)) {
      const firstMessageId = messages[0].message._id;

      return `${defaultKey}-${firstMessageId}`;
    }

    return defaultKey;
  };

  _onSortChange = (newSort: SortConfig[]) => {
    const { onConfigChange, config } = this.props;
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig);
  }

  static contextType = RenderCompletionCallback;

  render() {
    const {
      config,
      currentView: { activeQuery: activeQueryId },
      data: { messages, total: totalMessages },
      editing,
      fields,
      pageSize,
      selectedFields,
      setLoadingState,
    } = this.props;
    const { currentPage, errors } = this.state;
    const hasError = !isEmpty(errors);
    const listKey = this._getListKey();

    return (
      <Wrapper>
        <PaginatedList onChange={this._handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalMessages}
                       pageSize={pageSize}>
          {!hasError ? (
            <MessageTable activeQueryId={activeQueryId}
                          config={config}
                          editing={editing}
                          fields={fields}
                          key={listKey}
                          onSortChange={this._onSortChange}
                          selectedFields={selectedFields}
                          setLoadingState={setLoadingState}
                          messages={messages} />
          ) : <ErrorWidget errors={errors} />}
        </PaginatedList>
      </Wrapper>
    );
  }
}

export default connect(MessageList,
  {
    selectedFields: SelectedFieldsStore,
    currentView: ViewStore,
    searches: SearchStore,
  }, (props) => ({

    ...props,
    searchTypes: get(props, ['searches', 'result', 'results', props.currentView.activeQuery, 'searchTypes']),
  }));
