package org.biterp.admin1c.cmd;

import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import org.biterp.admin1c.console.AgentAdminUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockCommand implements Command {

    private final String server1c;
    private final int portRAS;
    private final String admin1cUsr;
    private final String admin1cPwd;
    private final String mode;
    private final Long timer;

    private final static Logger log = LoggerFactory.getLogger(SheduledJobsReceiver.class);

    public LockCommand(String server1c, int portRAS, String admin1cUsr, String admin1cPwd, String mode, Long timer) {
        this.server1c = server1c;
        this.portRAS = portRAS;
        this.admin1cUsr = admin1cUsr;
        this.admin1cPwd = admin1cPwd;
        this.mode = mode;
        this.timer = timer;
    }

    @Override
    public void execute() {

        try {
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
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1); // We are forced to end process manually because error in admin1c.connect() hangs app
        }
    }
}
