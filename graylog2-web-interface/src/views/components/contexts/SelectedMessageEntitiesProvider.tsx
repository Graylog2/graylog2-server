import type { SetStateAction } from 'react';
import * as React from 'react';
import { useMemo, useState, useCallback } from 'react';

import SelectEntitiesContext from 'components/common/EntityDataTable/contexts/SelectEntitiesContext';
import type { EntityBase } from 'components/common/EntityDataTable/types';

const removeSelectedEntityId = <Entity extends EntityBase>(
  selectedEntities: Array<Entity['id']>,
  targetEntityId: Entity['id'],
) => selectedEntities.filter((entityId) => entityId !== targetEntityId);

type Props<Entity extends EntityBase> = React.PropsWithChildren<{
  initialSelection?: Array<string>;
  onChangeSelection?: (selectedEntities: Array<Entity['id']>, data: Readonly<Array<Entity>>) => void;
  entities: Readonly<Array<Entity>>;
}>;

const isUpdateFunction = <Entity extends EntityBase>(
  setSelectedEntitiesArgument: SetStateAction<Array<Entity['id']>>,
): setSelectedEntitiesArgument is (prev: Array<Entity['id']>) => Array<Entity['id']> =>
  typeof setSelectedEntitiesArgument === 'function';

const SelectedMessageEntitiesProvider = <Entity extends EntityBase>({
  children = undefined,
  initialSelection = [],
  onChangeSelection = undefined,
  entities,
}: Props<Entity>) => {
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection);

  const _setSelectedEntities = useCallback(
    (setSelectedEntitiesArgument: SetStateAction<Array<Entity['id']>>) => {
      setSelectedEntities((currentSelectedEntities) => {
        const newState = isUpdateFunction<Entity>(setSelectedEntitiesArgument)
          ? setSelectedEntitiesArgument(currentSelectedEntities)
          : setSelectedEntitiesArgument;

        if (onChangeSelection) {
          onChangeSelection(newState, entities);
        }

        return newState;
      });
    },
    [entities, onChangeSelection],
  );

  const deselectEntity = useCallback(
    (targetEntityId: EntityBase['id']) => _setSelectedEntities((cur) => removeSelectedEntityId(cur, targetEntityId)),
    [_setSelectedEntities],
  );

  const selectEntity = useCallback(
    (targetEntityId: EntityBase['id']) => _setSelectedEntities((cur) => [...cur, targetEntityId]),
    [_setSelectedEntities],
  );

  const toggleEntitySelect = useCallback(
    (targetEntityId: EntityBase['id']) => {
      _setSelectedEntities((cur) => {
        if (cur.includes(targetEntityId)) {
          return removeSelectedEntityId(cur, targetEntityId);
        }

        return [...cur, targetEntityId];
      });
    },
    [_setSelectedEntities],
  );

  const visibleEntityIds = useMemo(() => entities.map(({ id }) => id), [entities]);
  const selectedVisibleEntitiesCount = useMemo(
    () => visibleEntityIds.filter((entityId) => selectedEntities.includes(entityId)).length,
    [selectedEntities, visibleEntityIds],
  );
  const isAllRowsSelected = visibleEntityIds.length > 0 && selectedVisibleEntitiesCount === visibleEntityIds.length;
  const isSomeRowsSelected = selectedVisibleEntitiesCount > 0 && !isAllRowsSelected;

  const contextValue = useMemo(
    () => ({
      setSelectedEntities: _setSelectedEntities,
      selectedEntities,
      deselectEntity,
      selectEntity,
      toggleEntitySelect,
      isAllRowsSelected,
      isSomeRowsSelected,
    }),
    [
      _setSelectedEntities,
      selectedEntities,
      deselectEntity,
      selectEntity,
      toggleEntitySelect,
      isAllRowsSelected,
      isSomeRowsSelected,
    ],
  );

  return <SelectEntitiesContext.Provider value={contextValue}>{children}</SelectEntitiesContext.Provider>;
};

export default SelectedMessageEntitiesProvider;
