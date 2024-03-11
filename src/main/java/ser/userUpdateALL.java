package ser;

import com.ser.blueline.AccountStatus;
import com.ser.blueline.IRole;
import com.ser.blueline.IUser;
import com.ser.blueline.LicenseType;
import com.ser.blueline.bpm.IWorkbasket;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class userUpdateALL extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    @Override
    protected Object execute() {
        log.info(" ---- Agent Started ----");
        try {
            IUser[] users = getDocumentServer().getUsers(getSes());

            for(IUser user : users) {

                if (user == null) continue;
                if (user.getLicenseType() == LicenseType.TECHNICAL_USER) continue;
                if (user.getAccountStatus() != AccountStatus.ACTIVE) continue;

                if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                    IUser tmp1 = user.getModifiableCopy(getSes());

                    String firstName = user.getFirstName();
                    String lastName = "";
                    if (user.getLastName() != null && !user.getLastName().isEmpty()) lastName = user.getLastName();
                    String[] names = lastName.split(" ");
                    if (names.length > 0 && names[0] != firstName) lastName = firstName + " " + lastName;

                    tmp1.setFirstName("");
                    tmp1.setLastName(lastName.trim());
                    tmp1.commit();

                }

                IWorkbasket workbasket = getUserWorkbasket(user);
                if(workbasket != null){
                    if(workbasket.getWorkbasketContentViewDefinitionID() == null){
                        log.info("Update Workbasket [" + workbasket.getFullName() + "] WorkbasketContentViewDefinitionID ");
                        IWorkbasket temp = workbasket.getModifiableCopy(getSes());
                        temp.setWorkbasketContentViewDefinitionID(Conf.ClassIDs.ProjectDocumentCycle);
                        temp.commit();
                    }
                }
            }

            IRole role = getDocumentServer().getRoleByName(getSes(), Conf.RoleNames.InternalDCC);
            if(role != null) {
                IUser[] usrs = role.getUserMembers();
                for (IUser usr : usrs) {
                    if (!usr.getLicenseType().equals(LicenseType.LIGHTWEIGHT_USER)) {
                        continue;
                    }

                    log.info("Update User [" + usr.getFullName() + "] licence type -- NORMAL_USER");
                    IUser tmp = usr.getModifiableCopy(getSes());
                    tmp.setLicenseType(LicenseType.NORMAL_USER);
                    tmp.commit();
                }
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished");

    }

    private IWorkbasket getUserWorkbasket(IUser user) {
        log.info("Getting User Workbasket");
        IWorkbasket wb = getBpm().getWorkbasketByAssociatedOrgaElement(user);
        if (wb == null) {
            log.info("User doesn't have an associated Worbasket");
            wb = associateWorbasketForUser(user);
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