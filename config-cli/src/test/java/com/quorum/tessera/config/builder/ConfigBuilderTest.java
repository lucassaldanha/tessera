package com.quorum.tessera.config.builder;


import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.JdbcConfig;
import com.quorum.tessera.config.SslAuthenticationMode;
import com.quorum.tessera.config.SslTrustMode;
import org.eclipse.persistence.jaxb.BeanValidationMode;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigBuilderTest {

    private final ConfigBuilder builderWithValidValues = ConfigBuilder.create()
            .jdbcConfig(new JdbcConfig("jdbcUsername", "jdbcPassword", "jdbc:bogus"))
            .peers(Collections.EMPTY_LIST)
            .serverPort(892)
            .sslAuthenticationMode(SslAuthenticationMode.STRICT)
            .unixSocketFile("somepath.ipc")
            .serverHostname("http://bogus.com:928")
            .sslServerKeyStorePath("sslServerKeyStorePath")
            .sslServerTrustMode(SslTrustMode.TOFU)
            .sslServerTrustStorePath("sslServerTrustStorePath")
            .sslServerTrustStorePath("sslServerKeyStorePath")
            .sslClientKeyStorePath("sslClientKeyStorePath")
            .sslClientTrustStorePath("sslClientTrustStorePath")
            .sslClientKeyStorePassword("sslClientKeyStorePassword")
            .sslClientTrustStorePassword("sslClientTrustStorePassword")
            .knownClientsFile("knownClientsFile")
            .knownServersFile("knownServersFile");

    @Test
    public void buildValid() {
        Config result = builderWithValidValues.build();

        assertThat(result).isNotNull();
    }

    @Test
    public void influxHostnameEmptyThenInfluxConfigIsNull() {
        Config result = builderWithValidValues.influxHostName("")
                                            .build();

        assertThat(result.getServerConfig().getInfluxConfig()).isNull();
    }

    /*
    Create config from existing config and ensure all 
    properties are populated using marhsalled values
     */
    @Test
    public void buildFromExisting() throws Exception {
        Config existing = builderWithValidValues.build();

        ConfigBuilder configBuilder = ConfigBuilder.from(existing);

        Config result = configBuilder.build();

        JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty("eclipselink.beanvalidation.mode", BeanValidationMode.NONE);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        final String expected;
        try (Writer writer = new StringWriter()) {
            marshaller.marshal(existing, writer);
            expected = writer.toString();
        }

        final String actual;
        try (Writer writer = new StringWriter()) {
            marshaller.marshal(result, writer);
            actual = writer.toString();
        }

        Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

        assertThat(diff.getDifferences()).isEmpty();

    }

}
