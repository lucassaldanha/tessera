package com.quorum.tessera.test.migration.config;

import com.quorum.tessera.config.*;
import com.quorum.tessera.config.keypairs.FilesystemKeyPair;
import com.quorum.tessera.config.util.JaxbUtil;
import cucumber.api.java8.En;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMigrationSteps implements En {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private Path outputFile;

    public ConfigMigrationSteps() {

        Given("^(.+) exists$", (String filePath) -> assertThat(getClass().getResource(filePath)).isNotNull());

        Given("^the outputfile is created$", () -> assertThat(Files.exists(outputFile)).isTrue());

        When(
                "the Config Migration Utility is run with tomlfile (.+) and --outputfile option",
                (String toml) -> {
                    final String jarfile = System.getProperty("config-migration-app.jar");

                    outputFile = Paths.get("target", UUID.randomUUID().toString());

                    assertThat(Files.exists(outputFile)).isFalse();

                    List<String> args =
                            new ArrayList<>(
                                    Arrays.asList(
                                            "java",
                                            "-jar",
                                            jarfile,
                                            "--tomlfile",
                                            getAbsolutePath(toml).toString(),
                                            "--outputfile",
                                            outputFile.toAbsolutePath().toString()));
                    System.out.println(String.join(" ", args));

                    ProcessBuilder configMigrationProcessBuilder = new ProcessBuilder(args);

                    final Process configMigrationProcess =
                            configMigrationProcessBuilder.redirectErrorStream(true).start();

                    executorService.submit(
                            () -> {
                                final InputStream inputStream = configMigrationProcess.getInputStream();
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.out.println(line);
                                    }
                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });

                    configMigrationProcess.waitFor();

                    if (configMigrationProcess.isAlive()) {
                        configMigrationProcess.destroy();
                    }
                });

        Then(
                "(.+) and the outputfile are equivalent",
                (String legacyPath) -> {
                    final Config migratedConfig = JaxbUtil.unmarshal(Files.newInputStream(outputFile), Config.class);

                    // TODO These values were retrieved from legacy.toml.  Ideally legacyConfig would be generated by
                    // unmarshalling legacy.toml but didn't want to use the toml unmarshalling production code in the
                    // test
                    final SslConfig sslConfig = new SslConfig();
                    sslConfig.setTls(SslAuthenticationMode.STRICT);
                    sslConfig.setServerTlsCertificatePath(Paths.get("data", "tls-server-cert.pem").toAbsolutePath());
                    sslConfig.setServerTlsKeyPath(Paths.get("data", "tls-server-key.pem").toAbsolutePath());
                    sslConfig.setServerTrustCertificates(Collections.emptyList());
                    sslConfig.setServerTrustMode(SslTrustMode.TOFU);
                    sslConfig.setKnownClientsFile(Paths.get("data", "tls-known-clients").toAbsolutePath());
                    sslConfig.setClientTlsCertificatePath(Paths.get("data", "tls-client-cert.pem").toAbsolutePath());
                    sslConfig.setClientTlsKeyPath(Paths.get("data", "tls-client-key.pem").toAbsolutePath());
                    sslConfig.setClientTrustCertificates(Collections.emptyList());
                    sslConfig.setClientTrustMode(SslTrustMode.CA_OR_TOFU);
                    sslConfig.setKnownServersFile(Paths.get("data", "tls-known-servers").toAbsolutePath());

                    final DeprecatedServerConfig server = new DeprecatedServerConfig();
                    server.setSslConfig(sslConfig);
                    server.setHostName("http://127.0.0.1");
                    server.setPort(9001);
                    server.setCommunicationType(CommunicationType.REST);

                    final KeyConfiguration keys = new KeyConfiguration();
                    keys.setKeyData(
                            Arrays.asList(
                                    new FilesystemKeyPair(
                                            Paths.get("data", "foo.pub").toAbsolutePath(),
                                            Paths.get("data", "foo.key").toAbsolutePath())));
                    keys.setPasswordFile(Paths.get("data", "passwords").toAbsolutePath());

                    final JdbcConfig jdbcConfig = new JdbcConfig();
                    jdbcConfig.setUrl("jdbc:h2:mem:tessera");

                    final Config legacyConfig = new Config();
                    legacyConfig.setServer(server);
                    legacyConfig.setPeers(Arrays.asList(new Peer("http://127.0.0.1:9000/")));
                    legacyConfig.setKeys(keys);
                    legacyConfig.setJdbcConfig(jdbcConfig);
                    legacyConfig.setUnixSocketFile(Paths.get("data", "constellation.ipc").toAbsolutePath());
                    legacyConfig.setAlwaysSendTo(Collections.emptyList());

                    assertThat(migratedConfig.getServer())
                            .isEqualToComparingFieldByFieldRecursively(legacyConfig.getServer());
                    assertThat(migratedConfig.getKeys())
                            .isEqualToComparingFieldByFieldRecursively(legacyConfig.getKeys());
                    assertThat(migratedConfig.getUnixSocketFile()).isEqualTo(legacyConfig.getUnixSocketFile());
                    assertThat(migratedConfig.getJdbcConfig())
                            .isEqualToComparingFieldByField(legacyConfig.getJdbcConfig());
                    assertThat(migratedConfig.getP2PServerConfig())
                            .isEqualToComparingFieldByFieldRecursively(legacyConfig.getP2PServerConfig());
                    assertThat(migratedConfig.getAlwaysSendTo()).isEqualTo(legacyConfig.getAlwaysSendTo());
                    assertThat(migratedConfig.getServerConfigs().size())
                            .isEqualTo(legacyConfig.getServerConfigs().size());
                    assertThat(migratedConfig.getServerConfigs()).hasSize(2);
                    assertThat(migratedConfig.getServerConfigs())
                            .usingRecursiveFieldByFieldElementComparator()
                            .containsExactlyInAnyOrder(
                                    legacyConfig.getServerConfigs().get(0), legacyConfig.getServerConfigs().get(1));
                    assertThat(migratedConfig.getPeers()).isEqualTo(legacyConfig.getPeers());
                });
    }

    private Path getAbsolutePath(String filePath) throws Exception {
        return Paths.get(getClass().getResource(filePath).toURI()).toAbsolutePath();
    }
}
