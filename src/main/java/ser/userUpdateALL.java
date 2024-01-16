package ser;

import com.ser.blueline.AccountStatus;
import com.ser.blueline.IUser;
import com.ser.blueline.LicenseType;
import com.ser.blueline.bpm.IWorkbasket;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class userUpdateALL extends UnifiedAgent {

    private Logger log = LogManager.getLogger();

   private IUser user;
   private IUser userCopy;
    private IWorkbasket workbasket;

    @Override
    protected Object execute() {
        log.info(" ---- Agent Started ----");
        try {
            IUser[] users = getDocumentServer().getUsers(getSes());

            for(IUser userA : users) {
                this.user=userA;

                if (user == null) continue;
                if (user.getLicenseType() == LicenseType.TECHNICAL_USER) continue;
                if (user.getAccountStatus() != AccountStatus.ACTIVE) continue;

                this.workbasket = getUserWorkbasket();
                if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                    userCopy = user.getModifiableCopy(getSes());

                    String firstName = user.getFirstName();
                    String lastName = "";
                    if (user.getLastName() != null && !user.getLastName().isEmpty()) lastName = user.getLastName();
                    String[] names = lastName.split(" ");
                    if (names.length > 0 && names[0] != firstName) lastName = firstName + " " + lastName;

                    userCopy.setFirstName("");
                    userCopy.setLastName(lastName.trim());
                    userCopy.commit();

                }
            }

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished");

    }

    private IWorkbasket getUserWorkbasket() {
        log.info("Getting User Workbasket");
        IWorkbasket wb = getBpm().getWorkbasketByAssociatedOrgaElement(user);
        if (wb == null) {
            log.info("User doesn't have an associated Worbasket");
            wb = associateWorbasketForUser(this.user);
        }
        return wb;
    }

    private IWorkbasket associateWorbasketForUser(IUser newUser) {
        IUser userCopy = newUser.getModifiableCopy(getSes());
        IWorkbasket wb = getBpm().createWorkbasketObject(userCopy);
        wb.commit();
        userCopy.commit();
        return wb;
    }
}