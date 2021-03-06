package org.biterp.admin1c.cmd;

import com._1c.v8.ibis.admin.*;
import org.biterp.admin1c.common.PerfCalc;
import org.biterp.admin1c.console.AgentAdminUtil;
import org.omg.PortableServer.THREAD_POLICY_ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.CollationElementIterator;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SheduledJobsReceiver {

    private final AgentAdminUtil admin1c;
    private final static Logger log = LoggerFactory.getLogger(SheduledJobsReceiver.class);

    public SheduledJobsReceiver(AgentAdminUtil admin1c) {
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

    public  void disconnect() {
        if (admin1c.isConnected()) {
            admin1c.disconnect();
        }
    }

    public  void lockUnlockInfobases(Long timer, boolean lock) {

        if (admin1c == null || !admin1c.isConnected()) {
            throw new IllegalStateException("The connection is not established.");
        }

        log.info("Invoking lockUnlockInfobases()...");

        long timeoutEnd = System.currentTimeMillis() + timer;
        while(timeoutEnd - System.currentTimeMillis() > 0 ) {
            IBResult procInfobases = getInfobasesListToBlock();
            setScheduledJobsDenied(lock ? procInfobases.getBasesForLocks() : procInfobases.getBasesForUnlocks(), lock);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("lockUnlockInfobases() completed");
    }

    public  IBResult getInfobasesListToBlock() {

        if (admin1c == null || !admin1c.isConnected()) {
            throw new IllegalStateException("The connection is not established.");
        }

        Map<IInfoBaseInfoShort, UUID> allBases = new HashMap<>();
        Map<IInfoBaseInfoShort, UUID> basesForUnlocks = new HashMap<>();
        Map<IInfoBaseInfoShort, UUID> basesForLocks = new HashMap<>();

        for (IClusterInfo cluster : admin1c.getClusterInfoList()) {

            final UUID clusterId = cluster.getClusterId();

            admin1c.getSessions(cluster.getClusterId()).stream().sequential()
                .filter(this::isNonClusterConnection)
                .collect(Collectors.toMap(sesn -> admin1c.getInfoBaseInfoShort(clusterId, sesn.getInfoBaseId()), sesn -> sesn))
                .entrySet()
                .stream()
                .filter(entry -> isNotIgnoredIb(entry.getKey()))
                .peek(entry -> addIfNotExists(entry.getKey(), allBases, clusterId))
                .filter(entry -> isClientConnection(entry.getValue()))
                .filter(entry -> isNonTemplateInfobase(entry.getKey()))
                .map(Map.Entry::getKey)
                .forEach((ib) -> addIfNotExists(ib, basesForUnlocks, clusterId));

            log.info("AllBases = {}", allBases.size());
            log.info("BasesForUnlocks = {}", basesForUnlocks.size());
            log.info("BasesForLocks = {}", basesForLocks.size());

            allBases.entrySet().stream().sequential()
                .filter(ib -> !findExisting(ib.getKey(), basesForUnlocks))
                .forEach((ib) -> addIfNotExists(ib.getKey(), basesForLocks, clusterId));
        }

        return new IBResult(allBases, basesForUnlocks, basesForLocks);
    }

    public  void setScheduledJobsDenied(Map<IInfoBaseInfoShort, UUID> ibList, boolean scheduledJobsDenied) {
        if (admin1c == null || !admin1c.isConnected()) {
            throw new IllegalStateException("The connection is not established.");
        }
        ibList.entrySet().stream().sequential()
            .forEach(ib -> setScheduledJobsDenied(ib.getKey(), ib.getValue(), scheduledJobsDenied));
    }

    public  void setScheduledJobsDenied(IInfoBaseInfoShort ib, UUID clusterId, boolean scheduledJobsDenied) {
        try {
            log.info("Processing infobase {} to {}...", ib.getName(), scheduledJobsDenied ? "lock" : "unlock");
            IInfoBaseInfo infobase = admin1c.getInfoBaseInfo(clusterId, ib.getInfoBaseId());
            if (infobase.isScheduledJobsDenied() == scheduledJobsDenied) {
                log.info("Infobase {} already {}. No actions performed", ib.getName(), scheduledJobsDenied ? "locked" : "unlocked");
                infobase.setScheduledJobsDenied(scheduledJobsDenied);
                admin1c.updateInfoBase(clusterId, infobase);
            } else {
                infobase.setScheduledJobsDenied(scheduledJobsDenied);
                admin1c.updateInfoBase(clusterId, infobase);
                log.info("Infobase {} {} successfully", ib.getName(), scheduledJobsDenied ? "locked" : "unlocked");
            }
        } catch (AgentAdminAuthenticationException e) {
            log.error("Cannot authentificate in infobase {} Failed to set scheduledJobsDenied to {}. Detailed info:{}", ib.getName(), e.getMessage());
        }
    }

    private  boolean isNonClusterConnection(ISessionInfo session) {
        return !(session.getAppId().equals("SrvrConsole") || session.getAppId().equals("RAS"));
    }

    private  boolean isNotIgnoredIb(IInfoBaseInfoShort ib) {
        return !ib.getDescr().toUpperCase().equals("IGNORE") && !ib.getDescr().toUpperCase().equals("IGNORE_ALL");
    }

    private  boolean isClientConnection(ISessionInfo session) {
        String appId = session.getAppId();
        return appId.equals("1CV8C") || appId.equals("1CV8") || appId.equals("WebClient");
    }

    private  boolean isNonTemplateInfobase(IInfoBaseInfoShort ib) {
        return !ib.getName().startsWith("template");
    }

    private  boolean findExisting(IInfoBaseInfoShort ib, Map<IInfoBaseInfoShort, UUID> map) {
        for (IInfoBaseInfoShort entry : map.keySet()) {
            if (entry.getInfoBaseId().equals(ib.getInfoBaseId())) {
                return true;
            }
        }
        return false;
    }

    private  void addIfNotExists(IInfoBaseInfoShort ib, Map<IInfoBaseInfoShort, UUID> ibList, UUID clusterId) {
        if (!findExisting(ib, ibList)) {
            ibList.put(ib, clusterId);
        }
    }
}
