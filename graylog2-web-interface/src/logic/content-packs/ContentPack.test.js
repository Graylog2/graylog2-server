import ContentPack from './ContentPack';

describe('ContentPack', () => {
  it('should remove a parameter from a content pack', () => {
    const parameterToRemove = {
      name: 'GELF_PORT',
      title: 'GELF Port',
      description: 'The port that should be used for the listening socket',
      type: 'integer',
      default_value: 12201,
    };
    const parameterToKeep = {
      name: 'GELF_ADDR',
      title: 'GELF ADDR',
      description: 'The address that should be used for the listening socket',
      type: 'string',
      default_value: 12201,
    };
    const oldContentPack = ContentPack.builder().parameters([parameterToRemove, parameterToKeep]).build();
    const newContentPack = oldContentPack.toBuilder().removeParameter(parameterToRemove).build();
    expect(newContentPack.parameters).toEqual([parameterToKeep]);
    expect(oldContentPack.parameters).toEqual([parameterToRemove, parameterToKeep]);
  });

  it('should add a parameter to a content pack', () => {
    const parameterToAdd = {
      name: 'GELF_PORT',
      title: 'GELF Port',
      description: 'The port that should be used for the listening socket',
      type: 'integer',
      default_value: 12201,
    };
    const parameterToKeep = {
      name: 'GELF_ADDR',
      title: 'GELF ADDR',
      description: 'The address that should be used for the listening socket',
      type: 'string',
      default_value: 12201,
    };
    const oldContentPack = ContentPack.builder().parameters([parameterToKeep]).build();
    const newContentPack = oldContentPack.toBuilder().addParameter(parameterToAdd).build();
    expect(newContentPack.parameters).toEqual([parameterToKeep, parameterToAdd]);
    expect(oldContentPack.parameters).toEqual([parameterToKeep]);
  });
});
