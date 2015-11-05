import jsRoutes from 'routing/jsRoutes';

const ExtractorTypes = {
  COPY_INPUT: 'Copy Input',
  GROK: 'Grok pattern',
  JSON: 'JSON',
  REGEX: 'Regular expression',
  REGEX_REPLACE: 'Replace with regular expression',
  SPLIT_AND_INDEX: 'Split & Index',
  SUBSTRING: 'Substring',
};

const ExtractorUtils = {
  EXTRACTOR_TYPES: Object.keys(ExtractorTypes).map(type => type.toLocaleLowerCase()),

  getNewExtractorRoutes(sourceNodeId, sourceInputId, fieldName, messageIndex, messageId) {
    const routes = {};
    this.EXTRACTOR_TYPES.forEach(extractorType => {
      routes[extractorType] = jsRoutes.controllers.ExtractorsController.newExtractor(sourceNodeId, sourceInputId, extractorType, fieldName, messageIndex, messageId).url;
    });

    return routes;
  },

  getReadableExtractorTypeName(extractorType) {
    return extractorType ? ExtractorTypes[extractorType.toLocaleUpperCase()] : extractorType;
  },
};

export default ExtractorUtils;
