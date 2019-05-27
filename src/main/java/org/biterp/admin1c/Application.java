package org.biterp.admin1c;

import com._1c.v8.ibis.admin.*;
import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import org.biterp.admin1c.cmd.*;
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
        args = new String[14];
        args[0] = "-server1c";
        args[1] = "devfinbase.bit-erp.loc";
        args[2] = "-portras";
        args[3] = "1545";
        args[4] = "-admin1c";
        args[5] = "Administrator";
        args[6] = "-pwd1c";
        args[7] = "111";
        args[8] = "-mode";
        args[9]= "unauthdrop";
        args[10]= "-timer";
        args[11]= "120000";
        args[12]= "-offscriptpath";
        //args[13]= "C:\\repository\\devops_sources\\copy_etalon\\set_offline_db.sql";
        */
        // including empty string after split()
        if (args.length == 0) {
            throw new IllegalArgumentException("Wrong parameters. You need to pass parameters: -server1c -portras -admin1c -pwd1c -mode -timer -offscriptpath");
        }

        Map<String, String> entries = new HashMap<>();
        for (int i = 0; i < args.length; i = i + 2) {
            entries.put(args[i], args[i + 1]);
        }

        String server1c = entries.get("-server1c");
        String admin1cUsr = entries.get("-admin1c");
        String admin1cPwd = entries.get("-pwd1c");
        String mode = entries.get("-mode");
        String offlineScriptPath = entries.get("-offscriptpath");
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

        Command cmd;
        if (mode.equals("lock") || mode.equals("unlock")) {
            cmd = new LockCommand(server1c, portRAS, admin1cUsr, admin1cPwd, mode, timer);
            cmd.execute();
        } else if (mode.equals("unauthdrop")) {
            cmd = new UnauthDropCommand(server1c, portRAS, admin1cUsr, admin1cPwd, offlineScriptPath);
            cmd.execute();
        } else {
            throw new IllegalArgumentException(String.format("Wrong parameter 'mode'=%s", mode));
        }

        perfCalc.end();
        log.info("Command {} finished successfully and took {}", mode, perfCalc.getTime());
    }
}
