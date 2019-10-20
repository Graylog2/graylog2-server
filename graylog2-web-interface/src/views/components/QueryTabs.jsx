import React from 'react';
import PropTypes from 'prop-types';

import { Tab, Tabs } from 'components/graylog';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import QueryTitle from 'views/components/queries/QueryTitle';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';

class QueryTabs extends React.Component {
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

  state = {
    showTitleEditModal: false,
    titleDraft: '',
  }

  _toggleTitleEditModal = (currentTitle) => {
    const { showTitleEditModal } = this.state;
    // Toggle edit modal depending on current state
    // We need to set the selected query title as the draft on open
    // and reset the draft input on close
    this.setState({
      showTitleEditModal: !showTitleEditModal,
      titleDraft: showTitleEditModal ? '' : currentTitle,
    });
  };

  _onTitleDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange, selectedQueryId } = this.props;
    onTitleChange(selectedQueryId, titleDraft);
    this._toggleTitleEditModal();
  };

  // eslint-disable-next-line no-undef
  _onTitleDraftChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    evt.preventDefault();
    evt.stopPropagation();
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { children, onSelect, onRemove, queries, selectedQueryId, titles, onSaveView, onSaveAsView } = this.props;
    const { showTitleEditModal, titleDraft } = this.state;
    const queryTitles = titles;
    const queryTabs = queries.map((id, index) => {
      const title = queryTitles.get(id, `Query#${index + 1}`);
      const tabTitle = (
        <QueryTitle active={id === selectedQueryId}
                    id={id}
                    onClose={() => onRemove(id)}
                    title={title}
                    toggleEditModal={this._toggleTitleEditModal} />
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
      <span>
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
        <QueryTitleEditModal onDraftChange={this._onTitleDraftChange}
                             onSave={this._onTitleDraftSave}
                             show={showTitleEditModal}
                             titleDraft={titleDraft}
                             toggleModal={this._toggleTitleEditModal} />
      </span>
    );
  }
}

export default QueryTabs;
