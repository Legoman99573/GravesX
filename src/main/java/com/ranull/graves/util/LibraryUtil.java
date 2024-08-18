/**
 * A utility class for loading libraries dynamically from Maven repositories.
 * <p>
 * This class is adapted from the `LibraryLoader` class found in the SpigotMC project.
 * For more details, see:
 * <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/plugin/java/LibraryLoader.java">
 * SpigotMC LibraryLoader Class</a>.
 * </p>
 */

package com.ranull.graves.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LibraryUtil {

    private final Logger logger;
    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;
    private ClassLoader classLoader;
    private static ClassLoader classLoaderStatic;
    private final File libsDir;

    public LibraryUtil(@NotNull Logger logger) {
        this.logger = logger;

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        this.repository = locator.getService(RepositorySystem.class);
        this.session = MavenRepositorySystemUtils.newSession();

        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        this.libsDir = new File("plugins/GravesX/libs");
        if (!libsDir.exists()) {
            libsDir.mkdirs();
        }
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, new LocalRepository(libsDir.getAbsolutePath())));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferStarted(@NotNull TransferEvent event) throws TransferCancelledException {
                logger.log(Level.INFO, "Downloading {0}", event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
            }
        });

        session.setSystemProperties(System.getProperties());
        session.setReadOnly();

        this.repositories = repository.newResolutionRepositories(session, Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()
        ));
    }

    public void loadDynamicLibrary(@NotNull String library) {
        List<String> libraries = Collections.singletonList(library);
        ClassLoader newLoader = null;
        try {
            newLoader = createLoader(libraries);
        } catch (MalformedURLException e) {
            // throw new RuntimeException(e);
        }

        if (newLoader != null) {
            this.classLoader = newLoader;
            classLoaderStatic = newLoader;
            Thread.currentThread().setContextClassLoader(newLoader);
            logger.log(Level.INFO, "Library loaded and class loader set");
        } else {
            logger.log(Level.SEVERE, "Failed to create class loader for library: {0}", library);
        }
    }

    /**
     * Creates a ClassLoader for the specified list of library coordinates.
     *
     * @param libraries List of library coordinates in the format "groupId:artifactId:version".
     * @return A ClassLoader that includes the specified libraries, or null if no valid libraries were loaded.
     */
    @Nullable
    private ClassLoader createLoader(@NotNull List<String> libraries) throws MalformedURLException {
        if (libraries.isEmpty()) {
            logger.log(Level.WARNING, "No libraries provided.");
            return null;
        }
        logger.log(Level.INFO, "Loading {0} libraries... please wait", libraries.size());

        List<Dependency> dependencies = new ArrayList<>();
        for (String library : libraries) {
            Artifact artifact = new DefaultArtifact(library);
            Dependency dependency = new Dependency(artifact, null);
            dependencies.add(dependency);
        }

        DependencyResult result;
        try {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setDependencies(dependencies);
            collectRequest.setRepositories(repositories);

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setCollectRequest(collectRequest);

            result = repository.resolveDependencies(session, dependencyRequest);
            logger.log(Level.INFO, "Dependencies resolved: {0}", result.getArtifactResults());
        } catch (DependencyResolutionException ex) {
            logger.log(Level.SEVERE, "Error resolving dependencies", ex);
            return null;
        }

        List<URL> jarFiles = new ArrayList<>();
        for (ArtifactResult artifactResult : result.getArtifactResults()) {
            Artifact artifact = artifactResult.getArtifact();
            File file = artifact.getFile();

            if (file == null || !file.exists()) {
                logger.log(Level.SEVERE, "Library file for artifact '{0}' is null or does not exist.", artifact.toString());
                continue;
            }

            File destFile = new File(libsDir, file.getName());

            // Copy the file to the plugins/GravesX/libs directory
            try (InputStream in = Files.newInputStream(file.toPath());
                 OutputStream out = Files.newOutputStream(destFile.toPath())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error copying library file to {0}", destFile.getAbsolutePath());
                continue;
            }

            URL url;
            try {
                url = destFile.toURI().toURL();
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, "Error converting library file to URL: " + destFile.getAbsolutePath(), ex);
                continue;
            }

            jarFiles.add(url);
            logger.log(Level.INFO, "Loaded library {0}", destFile.getAbsolutePath());
        }

        if (jarFiles.isEmpty()) {
            logger.log(Level.WARNING, "No valid library files were loaded.");
            return null;
        }

        // Convert List<URL> to URL[]
        List<URL> urls = new ArrayList<>();
        for (File lib : Objects.requireNonNull(libsDir.listFiles())) {
            if (lib.isFile() && lib.getName().endsWith(".jar")) {
                urls.add(lib.toURI().toURL());
            }
        }

        // Create and return the URLClassLoader with the array of URLs
        URL[] urlArray = urls.toArray(new URL[0]);
        URLClassLoader loader = new URLClassLoader(urlArray, this.getClass().getClassLoader());

        // Set the current thread's context ClassLoader
        Thread.currentThread().setContextClassLoader(loader);

        // Store class loaders for potential future use
        classLoader = loader;
        classLoaderStatic = loader;

        logger.log(Level.INFO, "Libraries loaded and class loader set");
        return loader;
    }

    @Nullable
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Nullable
    public static ClassLoader getClassLoaderStatic() {
        return classLoaderStatic;
    }
}