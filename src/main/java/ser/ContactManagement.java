package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IWorkbasket;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ContactManagement extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    @Override
    protected Object execute() {
        ISession ses = this.getSes();
        IDocumentServer srv = ses.getDocumentServer();
        ISerClassFactory classFactory = srv.getClassFactory();
        IBpmService bpmService = ses.getBpmService();

        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            IInformationObject parentDoc = ((IInformationObject) mainDocument).getPrimaryParent();
            String unitName = Conf.RoleNames.ExternalUsersUnit;
            IUnit unit = getDocumentServer().getUnitByName(getSes(), unitName);

            log.info("----OnChangeProjectCard Started ---for IDocument ID:--" + mainDocument.getID());
            String loginName = mainDocument.getDescriptorValue("PrimaryEMail");
            String password = "12345678";
            IUser user = getDocumentServer().getUserByLoginName(getSes(),loginName);
            if(user == null){
                user = classFactory.createUserInstance(getSes(),loginName,password);
                user.commit();
            }
            if(user!=null) {
                IUser cuser = user.getModifiableCopy(getSes());
                cuser.setLicenseType(LicenseType.LIGHTWEIGHT_USER);
                cuser.setFirstName(mainDocument.getDescriptorValue("PersonFirstName"));
                cuser.setLastName(mainDocument.getDescriptorValue("PersonLastName"));
                cuser.setEMailAddress(loginName);
                cuser.commit();
                log.info("External user created:" + cuser.getFullName());
                IWorkbasket wb = bpmService.getWorkbasketByAssociatedOrgaElement((IOrgaElement) cuser);
                if(wb == null) {
                    wb = bpmService.createWorkbasketObject((IOrgaElement) cuser);
                    wb.commit();
                    IWorkbasket wbCopy = wb.getModifiableCopy(getSes());
                    wbCopy.setNotifyEMail(loginName);
                    wbCopy.setOwner(cuser);
                    IRole admRole = getSes().getDocumentServer().getRoleByName(getSes(),"admins");
                    if(admRole != null) {
                        wbCopy.addAccessibleBy(admRole);
                    }
                    wbCopy.commit();
                }
                if(unit != null){
                    addToUnit(cuser,unit.getID());
                    log.info("add user:" + cuser.getFullName() + " to unit " + unitName);
                }
            }
            ses.refreshServerSessionCache();
            log.info("----Contact Updated --- for (User):" + loginName);
            log.info("----Contact Updated --- for (ID):" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void addToUnit(IUser user, String unitID) throws Exception {
        try {
            String[] unitIDs = (user != null ? user.getUnitIDs() : null);
            boolean isExist = Arrays.asList(unitIDs).contains(unitID);
            if(!isExist){
                List<String> rtrn = new ArrayList<String>(Arrays.asList(unitIDs));
                rtrn.add(unitID);
                IUser cuser = user.getModifiableCopy(getSes());
                String[] newUnitIDs = rtrn.toArray(new String[0]);
                cuser.setUnitIDs(newUnitIDs);
                cuser.commit();
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..addToRole : " + e);
        }
    }
}
