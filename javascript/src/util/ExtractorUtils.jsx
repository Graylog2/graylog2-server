import jsRoutes from 'routing/jsRoutes';

const ExtractorTypes = Object.freeze({
  COPY_INPUT: 'copy_input',
  GROK: 'grok',
  JSON: 'json',
  REGEX: 'regex',
  REGEX_REPLACE: 'regex_replace',
  SPLIT_AND_INDEX: 'split_and_index',
  SUBSTRING: 'substring',
});

const ExtractorUtils = {
  ExtractorTypes: ExtractorTypes,
  EXTRACTOR_TYPES: Object.keys(ExtractorTypes).map(type => type.toLocaleLowerCase()),

  getNewExtractorRoutes(sourceNodeId, sourceInputId, fieldName, messageIndex, messageId) {
    const routes = {};
    this.EXTRACTOR_TYPES.forEach(extractorType => {
      routes[extractorType] = jsRoutes.controllers.ExtractorsController.newExtractor(sourceNodeId, sourceInputId, extractorType, fieldName, messageIndex, messageId).url;
    });

    return routes;
  },

  getReadableExtractorTypeName(extractorType) {
    switch (extractorType) {
    case ExtractorTypes.COPY_INPUT:
      return 'Copy input';
    case ExtractorTypes.GROK:
      return 'Grok pattern';
    case ExtractorTypes.JSON:
      return 'JSON';
    case ExtractorTypes.REGEX:
      return 'Regular expression';
    case ExtractorTypes.REGEX_REPLACE:
      return 'Replace with regular expression';
    case ExtractorTypes.SPLIT_AND_INDEX:
      return 'Split & Index';
    case ExtractorTypes.SUBSTRING:
      return 'Substring';
    default:
      return extractorType;
    }
  },
};

export default ExtractorUtils;
