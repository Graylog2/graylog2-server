package org.graylog2.users;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PasswordAlgorithmFactoryTest {

    @Mock
    private PasswordAlgorithm passwordAlgorithm1;
    @Mock
    private PasswordAlgorithm passwordAlgorithm2;

    private Set<PasswordAlgorithm> passwordAlgorithmSet;

    @Before
    public void setUp() throws Exception {
        this.passwordAlgorithmSet = ImmutableSet.of(passwordAlgorithm1, passwordAlgorithm2);
    }

    @Test
    public void testForPasswordShouldReturnFirstAlgorithm() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(true);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(false);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithmSet, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isEqualTo(passwordAlgorithm1);
    }

    @Test
    public void testForPasswordShouldReturnSecondAlgorithm() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(false);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(true);

        final Set<PasswordAlgorithm> passwordAlgorithmSet = ImmutableSet.of(passwordAlgorithm1, passwordAlgorithm2);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithmSet, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isEqualTo(passwordAlgorithm2);
    }

    @Test
    public void testForPasswordShouldReturnNull() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(false);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(false);

        final Set<PasswordAlgorithm> passwordAlgorithmSet = ImmutableSet.of(passwordAlgorithm1, passwordAlgorithm2);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithmSet, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isNull();
    }

    @Test
    public void testDefaultPasswordAlgorithm() throws Exception {
        final PasswordAlgorithm defaultPasswordAlgorithm = mock(PasswordAlgorithm.class);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.<PasswordAlgorithm>emptySet(),
                defaultPasswordAlgorithm);

        assertThat(passwordAlgorithmFactory.defaultPasswordAlgorithm()).isEqualTo(defaultPasswordAlgorithm);
    }
}