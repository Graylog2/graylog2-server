package org.graylog.mcp.server;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.Collector;

public class OutputBuilder extends MarkdownBuilder {
    public OutputBuilder() {
        super();
    }

    public static Collector<String[], ?, String> toCsvString(String[] headers) {
        return Collector.of(
                () -> {
                    StringWriter sw = new StringWriter();
                    if (headers != null && headers.length > 0) {
                        try (CSVWriter writer = new CSVWriter(sw)) {
                            writer.writeNext(headers);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return sw;
                },
                (sw, row) -> {
                    try (CSVWriter writer = new CSVWriter(sw)) {
                        writer.writeNext(row);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (sw1, sw2) ->  {
                    sw1.write(sw2.toString());
                    return sw1;
                },
                StringWriter::toString
        );
    }
}
