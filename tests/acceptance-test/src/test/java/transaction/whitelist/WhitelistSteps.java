package transaction.whitelist;

import com.quorum.tessera.Launcher;
import com.quorum.tessera.config.AppType;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.util.JaxbUtil;
import com.quorum.tessera.io.FilesDelegate;
import com.quorum.tessera.test.DBType;
import com.quorum.tessera.test.Party;
import config.ConfigBuilder;
import cucumber.api.java8.En;
import exec.ExecArgsBuilder;
import exec.ExecUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import static org.assertj.core.api.Assertions.assertThat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suite.EnclaveType;
import suite.ExecutionContext;
import suite.ServerStatusCheck;
import suite.ServerStatusCheckExecutor;
import suite.SocketType;
import suite.Utils;

public class WhitelistSteps implements En {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistSteps.class);

    private static String jarPath = System.getProperty("application.jar", "../../tessera-app/target/tessera-app-0.9-SNAPSHOT-app.jar");

    private final URL logbackConfigFile = WhitelistSteps.class.getResource("/logback-node.xml");

    public WhitelistSteps() {
        
        final Path pid = Paths.get(System.getProperty("java.io.tmpdir"), "whitelist.pid");
        FilesDelegate.create().deleteIfExists(pid);
      
        FilesDelegate.create()
                .createFile(pid);
        
        pid.toFile().deleteOnExit();
       
        ExecutorService executorService = Executors.newCachedThreadPool();


        Given("Node at port {int}", (Integer port) -> {

            ExecutionContext executionContext = ExecutionContext.Builder.create()
                    .with(CommunicationType.REST)
                    .with(DBType.H2)
                    .with(EnclaveType.LOCAL)
                    .with(SocketType.HTTP)
                    .build();

            ConfigBuilder whiteListConfigBuilder = new ConfigBuilder()
                    .withNodeId("whitelist")
                    .withNodeNumbber(5)
                    .withQ2TSocketType(SocketType.HTTP)
                    .withExecutionContext(executionContext)
                    .withP2pPort(port)
                    .withPeer("http://localhost:7000")
                    .withKeys("WxsJ4souK0mptNx1UGw6hb1WNNIbPhLPvW9GoaXau3Q=", "YbOOFA4mwSSdGH6aFfGl2M7N1aiPOj5nHpD7GzJKSiA=");

            Config whiteListConfig = whiteListConfigBuilder.build();
            whiteListConfig.setUseWhiteList(true);

            Path configFile = Paths.get("target")
                    .resolve("white-list-config.json");

            try (OutputStream out = Files.newOutputStream(configFile)){
                JaxbUtil.marshalWithNoValidation(whiteListConfig, out);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            URL configUrl = Utils.toUrl(configFile);

            final Party party = new Party("WxsJ4souK0mptNx1UGw6hb1WNNIbPhLPvW9GoaXau3Q=", configUrl, "W");

            List<String> cmd = new ExecArgsBuilder()
                    .withMainClass(Launcher.class)
                    .withClassPathItem(Paths.get(jarPath))
                    .withConfigFile(configFile)
                    .withJvmArg("-Dlogback.configurationFile=" + logbackConfigFile)
                    .withJvmArg("-Dnode.number=whitelist")
                    .withPidFile(pid)
                    .build();
            
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            executorService.submit(() -> {
                try (BufferedReader reader = Stream.of(process.getInputStream())
                        .map(InputStreamReader::new)
                        .map(BufferedReader::new)
                        .findAny().get()){

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        LOGGER.info("Exec line Whitelist : {}", line);
                    }

                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });

            executorService.submit(() -> {
                try{
                    process.waitFor();
                } catch (InterruptedException ex) {

                }
            });

            CountDownLatch startUpLatch = new CountDownLatch(1);
    
           ServerStatusCheck serverStatusCheck = ServerStatusCheck
                    .create(whiteListConfig.getServerConfigs().stream()
                            .filter(s -> s.getApp() == AppType.P2P)
                            .findAny()
                            .get());
                    
            Boolean started = executorService.submit(new ServerStatusCheckExecutor(serverStatusCheck))
                    .get(20, TimeUnit.SECONDS);
                
            assertThat(started).isTrue();


        });

        List<Response> responseHolder = new ArrayList<>();
        When("a request is made against the node", () -> {

            Client client = ClientBuilder.newClient();
            Response response = client
                    .target("http://localhost:7000")
                    .path("upcheck")
                    .request()
                    .get();

            responseHolder.add(response);

        });

        Then("the response code is UNAUTHORIZED", () -> {
            assertThat(responseHolder.get(0).getStatus()).isEqualTo(401);
        });


        try{
            Files.lines(pid).findAny()
                    .ifPresent(ExecUtils::kill);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
