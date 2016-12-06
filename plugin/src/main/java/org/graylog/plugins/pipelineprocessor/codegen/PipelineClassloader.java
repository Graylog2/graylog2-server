package org.graylog.plugins.pipelineprocessor.codegen;

import java.util.concurrent.atomic.AtomicLong;

public class PipelineClassloader extends ClassLoader {

    public static AtomicLong loadedClasses = new AtomicLong();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        loadedClasses.incrementAndGet();
        return super.loadClass(name);
    }

    public void defineClass(String className, byte[] bytes) {
        super.defineClass(className, bytes, 0, bytes.length);
    }
}
