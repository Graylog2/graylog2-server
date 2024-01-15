import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';

const hasOverride = (fieldType: IndexSetFieldType) => fieldType.origin === 'OVERRIDDEN_PROFILE' || fieldType.origin === 'OVERRIDDEN_INDEX';

export default hasOverride;
