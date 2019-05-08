package org.biterp.admin1c;

import com._1c.v8.ibis.admin.*;
import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import org.biterp.admin1c.cmd.IBResult;
import org.biterp.admin1c.cmd.SheduledJobsReceiver;
import org.biterp.admin1c.common.PerfCalc;
import org.biterp.admin1c.console.AgentAdminUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.transport.tcp.TCPConnection;

import java.util.*;

public class Application {

    private final static PerfCalc perfCalc = new PerfCalc();
    private final static Logger log = LoggerFactory.getLogger(SheduledJobsReceiver.class);

    public static void main(String[] args) {

        /*
        args = new String[12];
        args[0] = "-server1c";
        args[1] = "devpglz.bit-erp.loc";
        args[2] = "-portras";
        args[3] = "1545";
        args[4] = "-admin1c";
        args[5] = "Administrator";
        args[6] = "-pwd1c";
        args[7] = "111";
        args[8] = "-mode";
        args[9]= "unlock";
        args[10]= "-timer";
        args[11]= "120000";
        */

        // including empty string after split()
        if (args.length != 12) {
            throw new IllegalArgumentException("Wrong parameters amount. You need to pass 6 paramters: -server1c -portras -admin1c -pwd1c -mode -timer");
        }

        Map<String, String> entries = new HashMap<>();
        for (int i = 0; i < args.length; i = i + 2) {
            entries.put(args[i], args[i + 1]);
        }

        String server1c = entries.get("-server1c");
        String admin1cUsr = entries.get("-admin1c");
        String admin1cPwd = entries.get("-pwd1c");
        String mode = entries.get("-mode");
        int portRAS;
        try {
            portRAS = Integer.parseInt(entries.get("-portras"));
        } catch (NumberFormatException e) {
            portRAS = 0;
        }
        Long timer;
        try {
            timer = Long.parseLong(entries.get("-timer"));
        } catch (NumberFormatException e) {
            timer = 0L;
        }

        perfCalc.start();

        SheduledJobsReceiver rec = new SheduledJobsReceiver(new AgentAdminUtil(new AgentAdminConnectorFactory()));
        rec.connect(server1c, portRAS, admin1cUsr, admin1cPwd);

        if (mode.equals("lock")) {
            rec.lockUnlockInfobases(timer, true);
        } else if (mode.equals("unlock")) {
            rec.lockUnlockInfobases(timer, false);
        } else {
            throw new IllegalArgumentException(String.format("Wrong parameter 'mode'=%s", mode));
        }
        rec.disconnect();

        perfCalc.end();
        log.info("Command {} finished successfully and took {}", mode, perfCalc.getTime());
    }
}
