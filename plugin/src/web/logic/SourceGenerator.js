const SourceGenerator = {
  generatePipeline(pipeline) {
    let source = `pipeline "${pipeline.title}"\n`;
    pipeline.stages.forEach(stage => {
      source += `stage ${stage.stage} match ${stage.match_all ? 'all' : 'either'}\n`;
      stage.rules.forEach(rule => {
        source += `rule "${rule}"\n`;
      });
    });
    source += 'end';

    return source;
  },
};

export default SourceGenerator;
