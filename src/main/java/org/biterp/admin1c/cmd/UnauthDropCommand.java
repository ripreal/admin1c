package org.biterp.admin1c.cmd;

import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import org.biterp.admin1c.console.AgentAdminUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UnauthDropCommand implements Command {

    private final String server1c;
    private final int portRAS;
    private final String admin1cUsr;
    private final String admin1cPwd;
    private final String offlineScriptPath;

    private final static Logger log = LoggerFactory.getLogger(UnauthDropCommand.class);

    public UnauthDropCommand(String server1c, int portRAS, String admin1cUsr, String admin1cPwd, String offlineScriptPath) {
        this.server1c = server1c;
        this.portRAS = portRAS;
        this.admin1cUsr = admin1cUsr;
        this.admin1cPwd = admin1cPwd;
        this.offlineScriptPath = offlineScriptPath;
    }

    @Override
    public void execute() {
        try {
            IbReceiver rec = new IbReceiver(new AgentAdminUtil(new AgentAdminConnectorFactory()));
            rec.connect(server1c, portRAS, admin1cUsr, admin1cPwd);
            rec.dropBases(server1c, Optional.ofNullable(offlineScriptPath).orElse("set_offline_db.sql"));
            rec.disconnect();
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1); // We are forced to end process manually because error in admin1c.connect() hangs app
        }
    }
}
