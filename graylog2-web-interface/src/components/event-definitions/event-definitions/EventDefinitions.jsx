import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import lodash from 'lodash';

import { Button, Col, DropdownButton, Label, MenuItem, Row } from 'components/graylog';
import Routes from 'routing/Routes';

import {
  EmptyEntity,
  EntityList,
  EntityListItem,
  IfPermitted,
  PaginatedList,
  SearchForm,
} from 'components/common';

import EventDefinitionDescription from './EventDefinitionDescription';
import styles from './EventDefinitions.css';

class EventDefinitions extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.array.isRequired,
    context: PropTypes.object,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEnable: PropTypes.func.isRequired,
    onDisable: PropTypes.func.isRequired,
  };

  static defaultProps = {
    context: {},
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
  };

  renderEmptyContent = () => {
    return (
      <Row>
        <Col md={6} mdOffset={3} lg={4} lgOffset={4}>
          <EmptyEntity>
            <p>
              Create Event Definitions that are able to search, aggregate or correlate Messages and other
              Events, allowing you to record significant Events in Graylog and alert on them.
            </p>
            <IfPermitted permissions="eventdefinitions:create">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success">Get Started!</Button>
              </LinkContainer>
            </IfPermitted>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  renderDescription = (definition, context) => {
    return <EventDefinitionDescription definition={definition} context={context} />;
  };

  render() {
    const { eventDefinitions, context, pagination, query, onPageChange, onQueryChange, onDelete, onEnable, onDisable } = this.props;

    if (pagination.grandTotal === 0) {
      return this.renderEmptyContent();
    }

    const items = eventDefinitions.map((definition) => {
      const isScheduled = lodash.get(context, `scheduler.${definition.id}.is_scheduled`, true);

      let toggle = <MenuItem onClick={onDisable(definition)}>Disable</MenuItem>;
      if (!isScheduled) {
        toggle = <MenuItem onClick={onEnable(definition)}>Enable</MenuItem>;
      }
      const actions = (
        <React.Fragment key={`actions-${definition.id}`}>
          <IfPermitted permissions={`eventdefinitions:edit:${definition.id}`}>
            <LinkContainer to={Routes.ALERTS.DEFINITIONS.edit(definition.id)}>
              <Button bsStyle="info">Edit</Button>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions={`eventdefinitions:delete:${definition.id}`}>
            <DropdownButton id="more-dropdown" title="More" pullRight>
              {toggle}
              <MenuItem divider />
              <MenuItem onClick={onDelete(definition)}>Delete</MenuItem>
            </DropdownButton>
          </IfPermitted>
        </React.Fragment>
      );

      const plugin = this.getConditionPlugin(definition.config.type);
      let titleSuffix = plugin.displayName || definition.config.type;

      if (!isScheduled) {
        titleSuffix = (<span>{titleSuffix} <Label bsStyle="warning">disabled</Label></span>);
      }

      return (
        <EntityListItem key={`event-definition-${definition.id}`}
                        title={definition.title}
                        titleSuffix={titleSuffix}
                        description={this.renderDescription(definition, context)}
                        noItemsText="Could not find any items with the given filter."
                        actions={actions} />
      );
    });

    return (
      <Row>
        <Col md={12}>
          <SearchForm query={query}
                      onSearch={onQueryChange}
                      onReset={onQueryChange}
                      searchButtonLabel="Find"
                      placeholder="Find Event Definitions"
                      wrapperClass={styles.inline}
                      queryWidth={200}
                      topMargin={0}
                      useLoadingState>
            <IfPermitted permissions="eventdefinitions:create">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success" className={styles.createButton}>Create Event Definition</Button>
              </LinkContainer>
            </IfPermitted>
          </SearchForm>

          <PaginatedList activePage={pagination.page}
                         pageSize={pagination.pageSize}
                         pageSizes={[10, 25, 50]}
                         totalItems={pagination.total}
                         onChange={onPageChange}>
            <div className={styles.definitionList}>
              <EntityList items={items} />
            </div>
          </PaginatedList>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitions;
