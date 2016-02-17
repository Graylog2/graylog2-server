package org.graylog.plugins.pipelineprocessor.functions.dates;

import com.google.common.collect.ImmutableList;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Optional;

public class FlexParseDate extends TimezoneAwareFunction {

    public static final String VALUE = "value";
    public static final String NAME = "flex_parse_date";
    public static final String DEFAULT = "default";

    @Override
    protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final String time = args.param(VALUE).evalRequired(args, context, String.class);
        final DateTimeZone timeZone = args.param(TIMEZONE).evalRequired(args, context, DateTimeZone.class);

        final List<DateGroup> dates = new Parser(timeZone.toTimeZone()).parse(time);
        if (dates.size() == 0) {
            final Optional<DateTime> defaultTime = args.param(DEFAULT).eval(args, context, DateTime.class);
            if (defaultTime.isPresent()) {
                return defaultTime.get();
            }
            // TODO really? this should probably throw an exception of some sort to be handled in the interpreter
            return null;
        }
        return new DateTime(dates.get(0).getDates().get(0), timeZone);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(
                ParameterDescriptor.string(VALUE).build(),
                ParameterDescriptor.type(DEFAULT, DateTime.class).optional().build()
        );
    }
}
