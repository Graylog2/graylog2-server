package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.ParameterException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PathSetConverterTest {
    private PathSetConverter converter;

    @Before
    public void setUp() {
        converter = new PathSetConverter();
    }

    @Test
    public void testConvertFrom() {
        Assert.assertEquals(0, converter.convertFrom("").size());
        Assert.assertEquals(0, converter.convertFrom(",").size());
        Assert.assertEquals(0, converter.convertFrom(",,,,,").size());
        Assert.assertEquals(1, converter.convertFrom("/another-dir").size());
        Assert.assertEquals(1, converter.convertFrom("/another-dir;/some-dir;/finally-dir").size());
        Assert.assertEquals(3, converter.convertFrom("/another-dir, /some-dir, /finally-dir").size());

        final Set<Path> result = converter.convertFrom("/a-dir");
        Assert.assertEquals("/a-dir", result.iterator().next().toString());
    }

    @Test(expected = ParameterException.class)
    public void testConvertFromNull() {
        converter.convertFrom(null);
    }

    @Test
    public void testConvertTo() {
        Set<Path> sortedSet = new HashSet<>();
        sortedSet.add(Paths.get("/some-dir"));
        sortedSet.add(Paths.get("/another-dir"));
        sortedSet.add(Paths.get("/finally-dir"));

        Assert.assertEquals("", converter.convertTo(new HashSet<Path>()));
        Assert.assertEquals("/another-dir,/some-dir,/finally-dir", converter.convertTo(sortedSet));
    }

    @Test(expected = ParameterException.class)
    public void testConvertToNull() {
        converter.convertTo(null);
    }
}
