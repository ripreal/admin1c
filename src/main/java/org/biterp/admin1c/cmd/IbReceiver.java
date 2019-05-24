package org.biterp.admin1c.cmd;

import com._1c.v8.ibis.admin.*;
import org.biterp.admin1c.console.AgentAdminUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IbReceiver {
    private final AgentAdminUtil admin1c;
    private final static Logger log = LoggerFactory.getLogger(SheduledJobsReceiver.class);

    public IbReceiver(AgentAdminUtil admin1c) {
        this.admin1c = admin1c;

    }

    public void connect(String adress, int port, String login, String password) {
        admin1c.connect(adress, port, 1);

        List<IClusterInfo> clusterInfo = admin1c.getClusterInfoList();
        for (IClusterInfo cluster : clusterInfo) {
            admin1c.authenticateCluster(cluster.getClusterId(), "", "");
            admin1c.addInfoBaseCredentials(cluster.getClusterId(), login, password);
            admin1c.addInfoBaseCredentials(cluster.getClusterId(), "Администратор", "111"); // for legacy compability
            admin1c.addInfoBaseCredentials(cluster.getClusterId(), "", "");
        }
    }

    public void dropBases(String serverSql, String offlineScriptPath) {
        if (admin1c == null || !admin1c.isConnected()) {
            throw new IllegalStateException("The connection is not established.");
        }

        for (IClusterInfo cluster : admin1c.getClusterInfoList()) {
            UUID clusterId = cluster.getClusterId();
            for (IInfoBaseInfoShort ibShort : admin1c.getInfoBaseShortInfos(clusterId)) {
                try {
                    log.info("processing {}...", ibShort.getName());
                    IInfoBaseInfo infobase = admin1c.getInfoBaseInfo(clusterId, ibShort.getInfoBaseId());
                } catch (AgentAdminAuthenticationException e) {
                    log.info("Found unauthorized infobase - {}...", ibShort.getName());
                    setDbOffline(serverSql, offlineScriptPath, ibShort.getName());
                    log.info("Deleting infobase - {}...", ibShort.getName());
                    admin1c.dropInfoBase(clusterId, ibShort.getInfoBaseId(), 0);
                    log.info("Infobase {} deleted", ibShort.getName());
                }
            }
        }
    }

    private void setDbOffline(String serverSql, String offlineScriptPath, String ibname) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            String cmdText = String.format("sqlcmd -S %s -E -i \"%s\" -b -v infobase =%s", serverSql, offlineScriptPath, ibname);
            log.debug("running command {}", cmdText);
            process = runtime.exec(cmdText);
            int returnCode = process.waitFor();
            if (returnCode != 0) {
                throw new RuntimeException(convert(process.getInputStream(), StandardCharsets.UTF_8));
            } else {
                log.info(convert(process.getInputStream(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public  void disconnect() {
        if (admin1c.isConnected()) {
            admin1c.disconnect();
        }
    }

    public String convert(InputStream inputStream, Charset charset) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}

