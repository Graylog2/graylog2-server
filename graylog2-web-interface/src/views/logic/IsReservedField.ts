import { RESERVED_FIELDS } from 'views/Constants';

const isReservedField = (fieldName: string) => RESERVED_FIELDS.includes(fieldName);

export default isReservedField;
