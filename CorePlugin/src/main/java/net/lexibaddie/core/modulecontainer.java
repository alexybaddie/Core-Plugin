package net.lexibaddie.core;

import java.io.File;
import java.net.URLClassLoader;

public class modulecontainer {
    private final File jarFile;
    private final moduleloader module;
    private final URLClassLoader classLoader;

    public modulecontainer(File jarFile, moduleloader module, URLClassLoader classLoader) {
        this.jarFile = jarFile;
        this.module = module;
        this.classLoader = classLoader;
    }

    public File getJarFile() {
        return jarFile;
    }

    public moduleloader getModule() {
        return module;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }
}
