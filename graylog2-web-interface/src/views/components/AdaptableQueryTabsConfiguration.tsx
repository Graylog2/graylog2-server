import * as React from 'react';
import type { Dispatch, SetStateAction } from 'react';
import { useCallback, useState } from 'react';
import { OrderedSet, Map, Set } from 'immutable';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { QueriesActions } from 'views/actions/QueriesActions';
import { SortableList } from 'components/common';
import type { ListItemType } from 'components/common/SortableList/ListItem';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import type { TitlesMap, TitleType } from 'views/stores/TitleTypes';
import type { QueryId } from 'views/logic/queries/Query';
import TitleTypes from 'views/stores/TitleTypes';

type Props = {
  show: boolean,
  setShow: Dispatch<SetStateAction<boolean>>,
  queriesList: Array<ListItemType>
}

const AdaptableQueryTabsConfiguration = ({ show, setShow, queriesList }: Props) => {
  const [orderedQueriesList, setOrderedQueriesList] = useState<Array<ListItemType>>(queriesList);
  const onSubmitTabsConfiguration = useCallback(() => {
    QueriesActions.setOrder(OrderedSet(orderedQueriesList)).then(() => {
      let newTitles = Set<{ queryId: QueryId, titlesMap: TitlesMap}>([]);

      orderedQueriesList.forEach(({ id: queryId, title }: {
        id: QueryId, title: string
      }) => {
        const titleMap = Map<string, string>({ title });
        const titlesMap = Map<TitleType, Map<string, string>>({ [TitleTypes.Tab]: titleMap });
        newTitles = newTitles.add({ queryId, titlesMap });
      });

      ViewStatesActions.patchQueriesTitle(newTitles);
      setShow(false);
    });
  }, [orderedQueriesList, setShow]);
  const onTabsConfigurationModalClose = useCallback(() => setShow(false), [setShow]);
  const updateTabSorting = useCallback((order) => {
    setOrderedQueriesList(order);
  }, [setOrderedQueriesList]);

  return (
    <BootstrapModalForm show={show}
                        title="Update Dashboard Tabs Configuration"
                        onSubmitForm={onSubmitTabsConfiguration}
                        onModalClose={onTabsConfigurationModalClose}
                        submitButtonText="Update configuration">
      <h3>Order</h3>
      <p>Use drag and drop to change the execution order of the dashboard tabs.</p>
      <SortableList items={orderedQueriesList} onMoveItem={updateTabSorting} displayOverlayInPortal />
    </BootstrapModalForm>
  );
};

export default AdaptableQueryTabsConfiguration;
