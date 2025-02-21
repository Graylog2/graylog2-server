import React, { useMemo } from 'react';

import {
  DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER,
} from 'components/users/UsersTokenManagement/constants';
import { fetchTokens, keyFn, Token } from 'components/users/UsersTokenManagement/hooks/useTokens';
import { PaginatedEntityTable } from 'components/common';
import CustomColumnRenderers from './ColumnRenderers';


const TokenManagement = () => {
  // const { entityActions } = useTableElements();
  const columnRenderers = useMemo(() => CustomColumnRenderers(), [])

  return (
    <PaginatedEntityTable<Token> humanName="token management"
                                    columnsOrder={COLUMNS_ORDER}
                                    additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                    actionsCellWidth={320}
                                    entityActions={() => (<></>)}
                                    tableLayout={DEFAULT_LAYOUT}
                                    fetchEntities={fetchTokens}
                                    keyFn={keyFn}
                                    entityAttributesAreCamelCase={false}
                                    columnRenderers={columnRenderers} />
  );
};

export default TokenManagement;
