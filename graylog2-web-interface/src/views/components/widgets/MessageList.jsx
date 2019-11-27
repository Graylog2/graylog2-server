// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

import { Messages } from 'views/Constants';
import { MessageTableEntry } from 'views/components/messagelist';
import Field from 'views/components/Field';
import { PaginatedList } from 'components/common';

import { AdditionalContext } from 'views/logic/ActionContext';
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { ViewStore } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { SearchActions } from 'views/stores/SearchStore';

import styles from './MessageList.css';
import RenderCompletionCallback from './RenderCompletionCallback';

const Wrapper = styled.div`
  display: grid;
  grid-template-rows: 1fr max-content;
  height: 100%;

  .pagination {
    margin-bottom: 0;
  }
`;

const { InputsActions } = CombinedProvider.get('Inputs');

type State = {
  currentPage: number,
  expandedMessages: Immutable.Set,
}

type Props = {
  fields: {},
  pageSize: number,
  config: MessagesWidgetConfig,
  data: { messages: [], total: number, id: string },
  selectedFields: {},
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
    selectedFields: PropTypes.object,
    currentView: PropTypes.object,
  };

  static defaultProps = {
    pageSize: Messages.DEFAULT_LIMIT,
    selectedFields: Immutable.Set(),
    currentView: { view: {}, activeQuery: undefined },
    config: undefined,
  };

  static contextType = RenderCompletionCallback;

  state = {
    currentPage: 1,
    expandedMessages: Immutable.Set(),
  };

  componentDidMount() {
    const onRenderComplete = this.context;
    InputsActions.list().then(() => (onRenderComplete && onRenderComplete()));
  }

  _getSelectedFields = () => {
    const { selectedFields, config } = this.props;
    if (config) {
      return Immutable.Set(config.fields);
    }
    return selectedFields;
  };

  _columnStyle = (fieldName) => {
    const { fields } = this.props;
    const selectedFields = Immutable.OrderedSet(fields);
    if (fieldName.toLowerCase() === 'source' && selectedFields.size > 1) {
      return { width: 180 };
    }
    return {};
  };

  _toggleMessageDetail = (id) => {
    let newSet;
    const { expandedMessages } = this.state;
    if (expandedMessages.contains(id)) {
      newSet = expandedMessages.delete(id);
    } else {
      newSet = expandedMessages.add(id);
      RefreshActions.disable();
    }
    this.setState({ expandedMessages: newSet });
  };

  _fieldTypeFor = (fieldName, fields: Immutable.List) => {
    return (fields.find(f => f.name === fieldName) || { type: FieldType.Unknown }).type;
  };

  _handlePageChange = (newPage: number) => {
    // execute search with new offset
    const { pageSize, data: { id: searchTypeId } } = this.props;
    const executionState = {
      global_override: {
        search_types: {
          [searchTypeId]: {
            limit: pageSize,
            offset: pageSize * (newPage - 1),
          },
        },
      },
      keep_search_types: [searchTypeId],
    };
    SearchActions.execute(executionState).then(() => {
      this.setState({
        currentPage: newPage,
      });
    });
  }

  render() {
    const { data, fields, currentView, pageSize = 7, config } = this.props;
    const messages = (data && data.messages) || [];
    const totalAmount = (data && data.total) || 0;
    const { currentPage, expandedMessages } = this.state;
    const messageSlice = messages
      .slice((currentPage - 1) * pageSize, currentPage * pageSize)
      .map((m) => {
        return {
          fields: m.message,
          formatted_fields: MessageFieldsFilter.filterFields(m.message),
          id: m.message._id,
          index: m.index,
          highlight_ranges: m.highlight_ranges,
        };
      });
    const selectedFields = this._getSelectedFields();
    const selectedColumns = Immutable.OrderedSet(selectedFields);
    const { activeQuery } = currentView;
    return (
      <Wrapper>
        <PaginatedList onChange={this._handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalAmount}
                       pageSize={pageSize}>
          <div className="search-results-table" style={{ overflow: 'auto' }}>
            <div className="table-responsive">
              <div className={`messages-container ${styles.messageListTableHeader}`}>
                <table className="table table-condensed messages" style={{ marginTop: 0 }}>
                  <thead>
                    <tr>
                      {selectedColumns.toSeq().map((selectedFieldName) => {
                        return (
                          <th key={selectedFieldName}
                              style={this._columnStyle(selectedFieldName)}>
                            <Field type={this._fieldTypeFor(selectedFieldName, fields)}
                                   name={selectedFieldName}
                                   queryId={activeQuery} />
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  {messageSlice.map((message) => {
                    const messageKey = `${message.index}-${message.id}`;
                    return (
                      <AdditionalContext.Provider key={messageKey}
                                                  value={{ message }}>
                        <MessageTableEntry fields={fields}
                                           disableSurroundingSearch
                                           message={message}
                                           showMessageRow={config && config.showMessageRow}
                                           selectedFields={selectedColumns}
                                           expanded={expandedMessages.contains(messageKey)}
                                           toggleDetail={this._toggleMessageDetail}
                                           highlight
                                           expandAllRenderAsync={false} />
                      </AdditionalContext.Provider>
                    );
                  })}
                </table>
              </div>
            </div>
          </div>
        </PaginatedList>
      </Wrapper>
    );
  }
}

export default connect(MessageList,
  {
    selectedFields: SelectedFieldsStore,
    currentView: ViewStore,
  });
