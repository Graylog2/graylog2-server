// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import connect from 'stores/connect';
import { isEmpty } from 'lodash';
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
  data: { messages: [], total: number, id: string },
  selectedFields: {},
  effectiveTimerange: TimeRange,
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
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig),
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
      total: PropTypes.number.isRequired,
      id: PropTypes.string.isRequired,
    }).isRequired,
    effectiveTimerange: PropTypes.shape({
      from: PropTypes.string.isRequired,
      to: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
    }).isRequired,
    selectedFields: PropTypes.object,
    currentView: PropTypes.object,
  };

  static defaultProps = {
    pageSize: Messages.DEFAULT_LIMIT,
    selectedFields: Immutable.Set(),
    currentView: { view: {}, activeQuery: undefined },
    config: undefined,
  };

  state = {
    errors: [],
    currentPage: 1,
  };

  componentDidMount() {
    const onRenderComplete = this.context;
    InputsActions.list().then(() => (onRenderComplete && onRenderComplete()));
  }


  _validPagesErrorMessage = (errors = []) => {
    const { pageSize } = this.props;
    const executionLimitError = errors.find(error => error.executionLimit);
    if (executionLimitError) {
      const { executionLimit } = executionLimitError;
      const validPages = Math.floor(executionLimit / pageSize);
      return { description: `With the current limit of ${executionLimit} and a page size of ${pageSize} messages, you can use the first ${validPages} pages.` };
    }
    return undefined;
  }

  _handlePageChange = (pageNo: number) => {
    // execute search with new offset
    const { pageSize, data: { id: searchTypeId }, effectiveTimerange } = this.props;
    const searchTypePayload = { [searchTypeId]: { limit: pageSize, offset: pageSize * (pageNo - 1) } };
    RefreshActions.disable();
    SearchActions.reexecuteSearchTypes(searchTypePayload, effectiveTimerange).then((response) => {
      let errors = [...response.result.errors];
      const validPagesInfo = this._validPagesErrorMessage(response.result.errors);
      if (validPagesInfo) {
        errors = [
          validPagesInfo,
          ...errors,
        ];
      }
      this.setState({
        errors,
        currentPage: pageNo,
      });
    });
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
    return (
      <Wrapper>
        <PaginatedList onChange={this._handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalMessages}
                       pageSize={pageSize}>
          {!hasError && <MessageTable messages={messages} fields={fields} config={config} selectedFields={selectedFields} activeQueryId={activeQueryId} key={`message-list-page-${currentPage}`} />}
          {hasError && (<ErrorWidget errors={errors} />)}
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
      effectiveTimerange: props.searches.result.results[props.currentView.activeQuery].effectiveTimerange,
    },
  ));
