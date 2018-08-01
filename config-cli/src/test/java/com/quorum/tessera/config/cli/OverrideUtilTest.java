package com.quorum.tessera.config.cli;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.Peer;
import com.quorum.tessera.config.SslTrustMode;
import com.quorum.tessera.config.util.JaxbUtil;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class OverrideUtilTest {

    @Test
    public void buildOptions() throws Exception {

        List<String> expected = Arrays.asList(
                "jdbc.username",
                "jdbc.password",
                "jdbc.url",
                "server.hostName",
                "server.port",
                "server.sslConfig.tls",
                "server.sslConfig.generateKeyStoreIfNotExisted",
                "server.sslConfig.serverKeyStore",
                "server.sslConfig.serverTlsKeyPath",
                "server.sslConfig.serverTlsCertificatePath",
                "server.sslConfig.serverKeyStorePassword",
                "server.sslConfig.serverTrustStore",
                "server.sslConfig.serverTrustStorePassword",
                "server.sslConfig.serverTrustMode",
                "server.sslConfig.clientKeyStore",
                "server.sslConfig.clientTlsKeyPath",
                "server.sslConfig.clientTlsCertificatePath",
                "server.sslConfig.clientKeyStorePassword",
                "server.sslConfig.clientTrustStore",
                "server.sslConfig.clientTrustStorePassword",
                "server.sslConfig.clientTrustMode",
                "server.sslConfig.knownClientsFile",
                "server.sslConfig.knownServersFile",
                "server.influxConfig.hostName",
                "server.influxConfig.port",
                "server.influxConfig.dbName",
                "server.influxConfig.pushIntervalInSecs",
                "peer.url",
                "keys.passwordFile",
                "keys.passwords",
                "keys.keyData.config.data.bytes",
                "keys.keyData.config.data.snonce",
                "keys.keyData.config.data.asalt",
                "keys.keyData.config.data.sbox",
                "keys.keyData.config.data.aopts.algorithm",
                "keys.keyData.config.data.aopts.iterations",
                "keys.keyData.config.data.aopts.memory",
                "keys.keyData.config.data.aopts.parallelism",
                "keys.keyData.config.data.password",
                "keys.keyData.config.type",
                "keys.keyData.privateKey",
                "keys.keyData.publicKey",
                "keys.keyData.privateKeyPath",
                "keys.keyData.publicKeyPath",
                "alwaysSendTo.key",
                "unixSocketFile",
                "useWhiteList",
                "server.sslConfig.clientTrustCertificates",
                "server.sslConfig.serverTrustCertificates"
        );

        Map<String, Class> results = OverrideUtil.buildConfigOptions();

        assertThat(results.keySet())
                .filteredOn(s -> !s.contains("$jacocoData"))
                .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(results.get("server.sslConfig.knownClientsFile")).isEqualTo(Path.class);
        assertThat(results.get("keys.passwords")).isEqualTo(String[].class);

    }

    @Test
    public void initialiseConfigFromNoValues() throws Exception {

        URL json = OverrideUtil.class.getResource("/init-values.json");

        Config config = JaxbUtil.unmarshal(json.openStream(), Config.class);

        assertThat(config).isNotNull();

          JaxbUtil.marshalWithNoValidation(config, System.out);
        OverrideUtil.overrideExistingValue(config, "useWhiteList", "true");
        OverrideUtil.overrideExistingValue(config, "jdbc.username", "someuser");
        OverrideUtil.overrideExistingValue(config, "jdbc.password", "somepassword");
        OverrideUtil.overrideExistingValue(config, "jdbc.url", "someurl");
        OverrideUtil.overrideExistingValue(config, "server.hostName", "somehost");
        OverrideUtil.overrideExistingValue(config, "server.port", "999");
        OverrideUtil.overrideExistingValue(config, "keys.passwords", "pw_one", "pw_two");

        OverrideUtil.overrideExistingValue(config, "server.sslConfig.clientKeyStorePassword", "SomeClientKeyStorePassword");

        OverrideUtil.overrideExistingValue(config, "server.sslConfig.clientTrustStore", "ClientTrustStore");

        OverrideUtil.overrideExistingValue(config, "server.sslConfig.clientTrustCertificates",
                "ClientTrustCertificates_1", "ClientTrustCertificates_2");

        OverrideUtil.overrideExistingValue(config, "server.sslConfig.clientTrustMode", "CA_OR_TOFU");

        OverrideUtil.overrideExistingValue(config, "server.influxConfig.pushIntervalInSecs", "987");

         OverrideUtil.overrideExistingValue(config, "peers.url", "PEER1","PEER2");
           JaxbUtil.marshalWithNoValidation(config, System.out);
        assertThat(config.getJdbcConfig()).isNotNull();
        assertThat(config.getJdbcConfig().getUsername()).isEqualTo("someuser");
        assertThat(config.getJdbcConfig().getPassword()).isEqualTo("somepassword");
        assertThat(config.getJdbcConfig().getUrl()).isEqualTo("someurl");

        assertThat(config.isUseWhiteList()).isTrue();

       assertThat(config.getPeers()).hasSize(2);
        assertThat(config.getKeys().getPasswords())
                .containsExactlyInAnyOrder("pw_one", "pw_two");

        assertThat(config.getServerConfig()).isNotNull();
        assertThat(config.getServerConfig().getHostName()).isEqualTo("somehost");
        assertThat(config.getServerConfig().getPort()).isEqualTo(999);

        assertThat(config.getServerConfig().getSslConfig().getClientKeyStorePassword())
                .isEqualTo("SomeClientKeyStorePassword");

        assertThat(config.getServerConfig().getSslConfig().getClientTrustStore())
                .isEqualTo(Paths.get("ClientTrustStore"));

        assertThat(config.getServerConfig().getSslConfig().getClientTrustMode())
                .isEqualTo(SslTrustMode.CA_OR_TOFU);

        assertThat(config.getServerConfig().getSslConfig().getClientTrustCertificates())
                .containsExactly(Paths.get("ClientTrustCertificates_1"), Paths.get("ClientTrustCertificates_2"));

        assertThat(config.getServerConfig().getInfluxConfig().getPushIntervalInSecs()).isEqualTo(987L);

    }

    @Test
    public void resolveFieldXmlElementName() {

        Field result = OverrideUtil.resolveField(SomeClass.class, "some_value");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("someValue");

    }

    @Test
    public void resolveField() {

        Field result = OverrideUtil.resolveField(SomeClass.class, "otherValue");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("otherValue");

    }

    static class SomeClass {

        @XmlElement(name = "some_value")
        private String someValue;

        @XmlElement
        private String otherValue;

    }

    enum Foo {
        INSTANCE
    }

    @Test
    public void isSimple() {

        assertThat(OverrideUtil.isSimple(int.class)).isTrue();
        assertThat(OverrideUtil.isSimple(boolean.class)).isTrue();
        assertThat(OverrideUtil.isSimple(long.class)).isTrue();
        assertThat(OverrideUtil.isSimple(Foo.class)).isTrue();
        assertThat(OverrideUtil.isSimple(String.class)).isTrue();
        assertThat(OverrideUtil.isSimple(Integer.class)).isTrue();
        assertThat(OverrideUtil.isSimple(Long.class)).isTrue();
        assertThat(OverrideUtil.isSimple(Boolean.class)).isTrue();
        assertThat(OverrideUtil.isSimple(List.class)).isFalse();

    }

    @Test
    public void toArrayType() {
        assertThat(OverrideUtil.toArrayType(String.class))
                .isEqualTo(String[].class);
        assertThat(OverrideUtil.toArrayType(Path.class))
                .isEqualTo(Path[].class);
    }

    @Test
    public void createInstance() {
        Peer result = OverrideUtil.createInstance(Peer.class);
        assertThat(result).isNotNull();
    }

    @Test
    public void classForName() {
        Class type = OverrideUtil.classForName(getClass().getName());
        assertThat(type).isEqualTo(getClass());
    }

}