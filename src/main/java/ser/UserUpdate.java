package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IWorkbasket;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class UserUpdate extends UnifiedAgent {

    private Logger log = LogManager.getLogger();

    @Override
    protected Object execute() {
        log.info(" ---- Agent Started ----");
        try {
            String userID = getEventInfObjID();
            if (userID == null) return resultError("Info Object ID is NULL");
            IUser user = getDocumentServer().getUser(getSes(), userID);

            if(user == null) return resultError("User not found");
            if(user.getLicenseType() == LicenseType.TECHNICAL_USER) return resultSuccess(user.getLogin() + " is technical user");
            if(user.getAccountStatus() != AccountStatus.ACTIVE) return resultSuccess(user.getLogin() + " is not active");;


//            if (user.getFirstName()!=null && !user.getFirstName().isEmpty()){
//                IUser tmp1 = user.getModifiableCopy(getSes());
//
//                String firstName = user.getFirstName();
//                String lastName = "";
//                if (user.getLastName()!=null && !user.getLastName().isEmpty()) lastName = user.getLastName();
//                String[] names= lastName.split(" ");
//                if(names.length>0 && names[0] != firstName) lastName = firstName +" " + lastName;
//
//                tmp1.setFirstName("");
//                tmp1.setLastName(lastName.trim());
//                tmp1.commit();
//
//            }

/*
            if (user.getLicenseType().equals(LicenseType.LIGHTWEIGHT_USER)) {
                String[] rids = user.getRoleIDs();
                if(rids != null && rids.length > 0){
                    IRole role = getDocumentServer().getRoleByName(getSes(), Conf.RoleNames.InternalDCC);
                    if(role != null) {
                        if(Arrays.asList(rids).contains(role.getID())){
                            log.info("Update User [" + user.getFullName() + "] licence type -- NORMAL_USER");
                            IUser tmp2 = user.getModifiableCopy(getSes());
                            tmp2.setLicenseType(LicenseType.NORMAL_USER);
                            tmp2.commit();
                        }
                    }
                }
            }*/

            IUser userCopy = user.getModifiableCopy(getSes());
            userCopy.setLicenseType(LicenseType.LIGHTWEIGHT_USER);
            userCopy.commit();




            IWorkbasket workbasket = getUserWorkbasket(user);
            if(workbasket != null){
                if(workbasket.getWorkbasketContentViewDefinitionID() == null){
                    //workbasket.setWorkbasketContentViewDefinitionID(Conf.ClassIDs.ProjectDocumentCycle);
                    IWorkbasket tmpWB = workbasket.getModifiableCopy(getSes());
                    tmpWB.setWorkbasketContentViewDefinitionID(Conf.ClassIDs.ProjectDocumentCycle);
                    ///Default WB sharing to admin -- start
                    Object[] orgElmnt = tmpWB.getAccessibleBy().toArray();
                    Set<IOrgaElement> orgElmn2 = new HashSet<>();
                    for(int i=0;i<orgElmnt.length;i++){
                        orgElmn2.add((IOrgaElement) orgElmnt[i]);
                    }
                    IRole role = getDocumentServer().getRoleByName(getSes(), "admins");
                    if(role != null) {
                        IOrgaElement orgRole = (IOrgaElement) role;
                        orgElmn2.add(orgRole);
                        tmpWB.setAccessibleBy(orgElmn2);
                    }
                    tmpWB.commit();
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