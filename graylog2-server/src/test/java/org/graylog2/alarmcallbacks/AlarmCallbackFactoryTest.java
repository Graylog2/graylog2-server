package org.graylog2.alarmcallbacks;

import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.shared.bindings.InstantiationService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackFactoryTest {
    private AlarmCallbackFactory alarmCallbackFactory;
    private InstantiationService instantiationService;
    private DummyAlarmCallback dummyAlarmCallback;

    public interface DummyAlarmCallback extends AlarmCallback {
    }

    @BeforeMethod
    public void setUp() throws Exception {
        this.instantiationService = mock(InstantiationService.class);
        this.dummyAlarmCallback = mock(DummyAlarmCallback.class);
        when(instantiationService.getInstance(DummyAlarmCallback.class)).thenReturn(dummyAlarmCallback);
        Set<Class<? extends AlarmCallback>> availableAlarmCallbacks = new HashSet<Class<? extends AlarmCallback>>();
        availableAlarmCallbacks.add(DummyAlarmCallback.class);

        this.alarmCallbackFactory = new AlarmCallbackFactory(instantiationService, availableAlarmCallbacks);
    }

    @Test
    public void testCreateByAlarmCallbackConfiguration() throws Exception {
        AlarmCallbackConfiguration configuration = mock(AlarmCallbackConfiguration.class);
        when(configuration.getType()).thenReturn(DummyAlarmCallback.class.getCanonicalName());

        AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);

        assertNotNull(alarmCallback);
        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }

    @Test
    public void testCreateByClassName() throws Exception {
        String className = DummyAlarmCallback.class.getCanonicalName();

        AlarmCallback alarmCallback = alarmCallbackFactory.create(className);

        assertNotNull(alarmCallback);
        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }

    @Test
    public void testCreateByClass() throws Exception {
        AlarmCallback alarmCallback = alarmCallbackFactory.create(DummyAlarmCallback.class);

        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }
}
