// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import connect from 'stores/connect';
import { isEmpty, get } from 'lodash';
import CombinedProvider from 'injection/CombinedProvider';

import { Messages } from 'views/Constants';

import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchActions, SearchStore } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';

import { PaginatedList } from 'components/common';
import CustomPropTypes from 'views/components/CustomPropTypes';
import MessageTable from 'views/components/widgets/MessageTable';
import ErrorWidget from 'views/components/widgets/ErrorWidget';

import RenderCompletionCallback from './RenderCompletionCallback';

const { InputsActions } = CombinedProvider.get('Inputs');

const Wrapper = styled.div`
  display: grid;
  grid-template-rows: 1fr max-content;
  height: 100%;

  .pagination {
    margin-bottom: 0;
  }
`;

type State = {
  errors: Array<{ description: string }>,
  currentPage: number,
}

type Props = {
  fields: {},
  pageSize: number,
  config: MessagesWidgetConfig,
  data: { messages: Array<Object>, total: number, id: string },
  selectedFields?: {},
  searchTypes: { [searchTypeId: string]: { effectiveTimerange: TimeRange }},
  setLoadingState: (loading: boolean) => void,
  currentView: {
    activeQuery: string,
    view: {
      id: number,
    },
  },
};

class MessageList extends React.Component<Props, State> {
  static propTypes = {
    fields: CustomPropTypes.FieldListType.isRequired,
    pageSize: PropTypes.number,
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
      total: PropTypes.number.isRequired,
      id: PropTypes.string.isRequired,
    }).isRequired,
    searchTypes: PropTypes.object.isRequired,
    selectedFields: PropTypes.object,
    setLoadingState: PropTypes.func.isRequired,
    currentView: PropTypes.object.isRequired,
  };

  static defaultProps = {
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
  }

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
  }

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
  }

  static contextType = RenderCompletionCallback;

  render() {
    const {
      config,
      currentView: { activeQuery: activeQueryId },
      data: { messages, total: totalMessages },
      fields,
      pageSize,
      selectedFields,
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
            <MessageTable messages={messages}
                          fields={fields}
                          config={config}
                          selectedFields={selectedFields}
                          activeQueryId={activeQueryId}
                          key={listKey} />
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
  }, props => Object.assign(
    {},
    props,
    {
      searchTypes: get(props, ['searches', 'result', 'results', props.currentView.activeQuery, 'searchTypes']),
    },
  ));
