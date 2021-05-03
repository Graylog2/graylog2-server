import { useQuery } from 'react-query';

import { TimeRange } from 'views/logic/queries/Query';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import FieldTypeMapping, { FieldTypeMappingJSON } from 'views/logic/fieldtypes/FieldTypeMapping';

const fieldTypesUrl = qualifyUrl('/views/fields');

type FieldTypesResponse = Array<FieldTypeMappingJSON>;

const _deserializeFieldTypes = (response: FieldTypesResponse) => response
  .map((fieldTypeMapping) => FieldTypeMapping.fromJSON(fieldTypeMapping));
const fetchAllFieldTypes = (streams: Array<string>, timerange: TimeRange): Promise<Array<FieldTypeMapping>> => fetch('POST', fieldTypesUrl, { streams, timerange })
  .then(_deserializeFieldTypes);

const useFieldTypes = (streams: Array<string> = [], timerange: TimeRange) => useQuery([streams, timerange], () => fetchAllFieldTypes(streams, timerange));

export default useFieldTypes;
