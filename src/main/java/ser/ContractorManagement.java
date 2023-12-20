package ser;

import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;

import java.util.*;

public class ContractorManagement extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    String prjCode = "";
    String compShortName = "";
    String compName = "";
    String paramName = "";
    String compIsMain = "";
    @Override
    protected Object execute() {
        ISession ses = this.getSes();
        IDocumentServer srv = ses.getDocumentServer();
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            log.info("----OnChangeProjectCard Started ---for IDocument ID:--" + mainDocument.getID());

            paramName       = "CCM_PARAM_CONTRACTOR-MEMBERS";
            prjCode         = mainDocument.getDescriptorValue("ccmPRJCard_code");
            compName        = mainDocument.getDescriptorValue("ObjectName");
            compShortName   = mainDocument.getDescriptorValue("ContactShortName");
            compIsMain      = mainDocument.getDescriptorValue("ccmPRJCard_status");

///--- MY (MAIN) COMPANY ISE UPDATE YAPILMAYACAK
            if(!Objects.equals(compIsMain, "1")) {
                if(prjCode == null || prjCode == ""){
                    throw new Exception("Exeption Caught...prjCode is NULL or EMPTY");
                }
                if(compName == null || compName == ""){
                    throw new Exception("Exeption Caught..contractor Name is NULL or EMPTY");
                }
                updateMembers2GVList(mainDocument);
                log.info("----Contractor Updated Project Members GVList ---for (ID):" + mainDocument.getID());

                updateUnit(getContractorMembersFromGVlist(compShortName));
                log.info("----Contractor updated Units ---for IDocument ID:--" + mainDocument.getID());

                updateRole(getMembersFromGVlist());
                log.info("----Contractor Updated Roles from ProjectCard ---for (ID):" + mainDocument.getID());
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }

    public void updatePrjCardGVList(String paramName, String paramKey, IDocument doc) throws Exception {
        try {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
            String rowValue = "";
            IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                rowValue = settingsMatrix.getValue(i, 0);
                if (rowValue.equalsIgnoreCase(paramKey)) {
                    srtMatrixModify.removeRow(i);
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                }
            }
            srtMatrixModify.appendRow();
            settingsMatrix.refresh();

            int rowCount = settingsMatrix.getRowCount();
            srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
            srtMatrixModify.setValue(rowCount, 1, doc.getDescriptorValue("ccmPRJCard_name"), false);
            srtMatrixModify.setValue(rowCount, 2, doc.getDescriptorValue("ccmContractNumber"), false);
            srtMatrixModify.setValue(rowCount, 3, doc.getDescriptorValue("ccmPRJCard_status"), false);
            srtMatrixModify.setValue(rowCount, 4, doc.getDescriptorValue("ccmPRJCard_country"), false);
            srtMatrixModify.setValue(rowCount, 5, doc.getDescriptorValue("ccmPrjDocClientPrjNumber"), false);
            srtMatrixModify.setValue(rowCount, 6, doc.getDescriptorValue("ccmPrjDocClient"), false);
            srtMatrixModify.setValue(rowCount, 7, doc.getDescriptorValue("ccmPRJCard_prefix"), false);
            srtMatrixModify.setValue(rowCount, 8, doc.getDescriptorValue("ccmPRJCard_ResponseDay"), false);
            srtMatrixModify.setValue(rowCount, 9, doc.getDescriptorValue("ccmPRJCard_ResponseDaySecond"), false);
            srtMatrixModify.setValue(rowCount, 10, doc.getDescriptorValue("ccmPRJCard_ConsalidatorDrtn"), false);
            srtMatrixModify.setValue(rowCount, 11, doc.getDescriptorValue("ccmPRJCard_DCCDrtn"), false);
            srtMatrixModify.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardGVList: " + e);
        }
    }
    public void updateMembers2GVList(IDocument doc) throws Exception {
        try {
            String ToReceiver = doc.getDescriptorValue("To-Receiver");
            String CcReceiver = doc.getDescriptorValue("CC-Receiver");
            String ObjectAuthors = doc.getDescriptorValue("ObjectAuthors");

            String membersTo = "";
            String membersCc = "";
            String membersAuthors = "";
            String[] membersToReceiverIDs = new String[0];
            String[] membersCcReceiverIDs = new String[0];
            String[] membersOuthorIDs = new String[0];

            if(ToReceiver != null) {
                membersTo = ToReceiver.replace("[", "").replace("]", "");
                membersToReceiverIDs = membersTo.split(",");
            }
            if(CcReceiver != null) {
                membersCc = CcReceiver.replace("[", "").replace("]", "");
                membersCcReceiverIDs = membersCc.split(",");
            }
            if(ObjectAuthors != null) {
                membersAuthors = ObjectAuthors.replace("[", "").replace("]", "");
                membersOuthorIDs = membersAuthors.split(",");
            }

            List<String> memberList = new ArrayList<>();
            memberList.addAll(Arrays.asList(membersToReceiverIDs));
            memberList.addAll(Arrays.asList(membersCcReceiverIDs));
            memberList.addAll(Arrays.asList(membersOuthorIDs));

            boolean isRemove = removeByPrjCodeFromGVList();

            int rowCount = 0;
            if(!memberList.isEmpty()) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                for (String memberName : memberList) {
                    IUser mmbr = getDocumentServer().getUserByLoginName(getSes(),memberName);
                    srtMatrixModify.appendRow();
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    rowCount = settingsMatrix.getRowCount()-1;
                    srtMatrixModify.setValue(rowCount, 0, prjCode, false);
                    srtMatrixModify.setValue(rowCount, 1, compShortName, false);
                    srtMatrixModify.setValue(rowCount, 2, compName, false);
                    srtMatrixModify.setValue(rowCount, 3, mmbr.getFullName(), false);
                    srtMatrixModify.setValue(rowCount, 4, memberName, false);
                    srtMatrixModify.setValue(rowCount, 5, mmbr.getID(), false);
                    srtMatrixModify.commit();
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardMembers2GVList: " + e);
        }
    }
    public boolean removeByPrjCodeFromGVList(){
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValuePrjCode = "";
        String rowValueCompSName = "";
        IStringMatrixModifiable srtMatrixModify = settingsMatrix.getModifiableCopy(getSes());
        for(int i = 0; i < srtMatrixModify.getRowCount(); i++) {
            rowValuePrjCode = srtMatrixModify.getValue(i, 0);
            rowValueCompSName = srtMatrixModify.getValue(i, 1);
            if (rowValuePrjCode.equalsIgnoreCase(prjCode) && rowValueCompSName.equalsIgnoreCase(compShortName)) {
                srtMatrixModify.removeRow(i);
                srtMatrixModify.commit();
                if(removeByPrjCodeFromGVList()){break;}
            }
        }
        return true;
    }
    public boolean existPRJGVList(String paramName, String key1) {
        boolean rtrn = false;
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValueParamKey = "";
        for(int i = 0; i < settingsMatrix.getRowCount(); i++) {
            rowValueParamKey = settingsMatrix.getValue(i, 2);
            if (rowValueParamKey.equalsIgnoreCase(key1)) {
                return true;
            }
        }
        return rtrn;
    }
    public void updateRole(List<String> members) throws Exception {
        try {
            //IRole dccRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.DCCUsersRole);
            IRole prjRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.ContractorUsersRole);
            List<String> prjRoleUserIDs = new ArrayList<>();
            if(prjRole == null){
                throw new Exception("Exeption Caught..updateRolesFromGVList..prjRole is NULL");
            }

            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
            if(settingsMatrix!=null) {
                for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                    String userId = settingsMatrix.getValue(i, 5);
                    IUser user = getDocumentServer().getUser(getSes() , userId);
                    if(user!=null) {
                        addToRole(user, prjRole.getID());
                    }
                }
            }

            IUser[] prjRoleMembers = prjRole.getUserMembers();
            if (prjRoleMembers != null) {
                for (IUser pMember : prjRoleMembers) {
                    prjRoleUserIDs.add(pMember.getID());
                }
            }
            for (String prjUserID : prjRoleUserIDs) {
                IUser prjRoleUser = getDocumentServer().getUser(getSes(), prjUserID);
                if (!members.contains(prjUserID)) {
                    removeFromRole(prjRoleUser,prjRole.getID());
                    log.info("removed user:" + prjRoleUser.getFullName() + " from role:" + prjRole.getName());
                }
            }

        }catch (Exception e){
            throw new Exception("Exeption Caught..updateRole: " + e);
        }
    }
    public void updateUnit(List<String> members) throws Exception {
        try {
            List<String> prjUnitUserIDs = new ArrayList<>();
            ISerClassFactory classFactory = getDocumentServer().getClassFactory();

            String unitName = prjCode + "_" + compShortName;
            IUnit unit = getDocumentServer().getUnitByName(getSes(), unitName);
            IUnit punit = getDocumentServer().getUnitByName(getSes(), prjCode);
            if(punit != null){
                if(unit == null){
                    unit = classFactory.createUnitInstance(getSes(),unitName);
                    unit.commit();
                    IUnit cunit = unit.getModifiableCopy(getSes());
                    cunit.setParent(punit);
                    cunit.commit();
                }
                if(unit != null){
                    if(unit.getParent() == null || (unit.getParent() != null && !Objects.equals(unit.getParent().getID(), punit.getID()))) {
                        IUnit cunit = unit.getModifiableCopy(getSes());
                        cunit.setParent(punit);
                        cunit.commit();
                    }

                    log.info("Unit update start:" + unit.getName());
                    for (String memberID : members) {
                        IUser memberUser = getDocumentServer().getUser(getSes(), memberID);
                        if (memberUser != null) {
                            addToUnit(memberUser,unit.getID());
                            log.info("add user:" + memberUser.getFullName() + " to unit " + unitName);
                        }
                    }

                    IUser[] prjUnitMembers = unit.getUserMembers();
                    if (prjUnitMembers != null) {
                        for (IUser pMember : prjUnitMembers) {
                            prjUnitUserIDs.add(pMember.getID());
                        }
                    }
                    for (String prjUserID : prjUnitUserIDs) {
                        IUser prjUnitUser = getDocumentServer().getUser(getSes(), prjUserID);
                        if (!members.contains(prjUserID)) {
                            removeFromUnit(prjUnitUser,unit.getID());
                            log.info("removed user:" + prjUnitUser.getFullName() + " from unit:" + unitName);
                        }
                    }
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updateUnit: " + e);
        }
    }
    public void addToRole(IUser user, String roleID) throws Exception {
        try {
            String[] roleIDs = (user != null ? user.getRoleIDs() : null);
            boolean isExist = Arrays.asList(roleIDs).contains(roleID);
            if(!isExist){
                List<String> rtrn = new ArrayList<String>(Arrays.asList(roleIDs));
                rtrn.add(roleID);
                IUser cuser = user.getModifiableCopy(getSes());
                String[] newRoleIDs = rtrn.toArray(new String[0]);
                cuser.setRoleIDs(newRoleIDs);
                cuser.commit();
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..addToRole : " + e);
        }
    }
    public void removeFromRole(IUser user, String roleID) throws Exception {
        try {
            String[] roleIDs = (user != null ? user.getRoleIDs() : null);
            List<String> rtrn = new ArrayList<String>(Arrays.asList(roleIDs));
            for (int i = 0; i < roleIDs.length; i++) {
                String rID = roleIDs[i];
                if (Objects.equals(rID, roleID)) {
                    rtrn.remove(roleID);
                }
            }
            IUser cuser = user.getModifiableCopy(getSes());
            String[] newRoleIDs = rtrn.toArray(new String[0]);
            cuser.setRoleIDs(newRoleIDs);
            cuser.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..removeFromRole : " + e);
        }
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
    public void removeFromUnit(IUser user, String unitID) throws Exception {
        try {
            String[] unitIDs = (user != null ? user.getUnitIDs() : null);
            List<String> rtrn = new ArrayList<String>(Arrays.asList(unitIDs));
            for (int i = 0; i < unitIDs.length; i++) {
                String rID = unitIDs[i];
                if (Objects.equals(rID, unitID)) {
                    rtrn.remove(unitID);
                }
            }
            IUser cuser = user.getModifiableCopy(getSes());
            String[] newUnitIDs = rtrn.toArray(new String[0]);
            cuser.setUnitIDs(newUnitIDs);
            cuser.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..removeFromUnit : " + e);
        }
    }
    public List<String> getMembersFromGVlist() throws Exception {
        List<String> prjUsers = new ArrayList<>();

        IRole prjRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.ContractorUsersRole);

        if(prjRole == null){
            throw new Exception("Exeption Caught..updateRolesFromGVList..prjRole is NULL");
        }

        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        if(settingsMatrix!=null) {
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String userId = settingsMatrix.getValue(i, 5);
                prjUsers.add(userId);
            }
        }
        return prjUsers;
    }
    public List<String> getContractorMembersFromGVlist(String contractorName) throws Exception {
        List<String> prjUsers = new ArrayList<>();
        IRole prjRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.ContractorUsersRole);
        if(prjRole == null){
            throw new Exception("Exeption Caught..updateRolesFromGVList..prjRole is NULL");
        }
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        if(settingsMatrix!=null) {
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowValuePrjCode = settingsMatrix.getValue(i, 0);
                String rowValueCompSName = settingsMatrix.getValue(i, 1);
                if (rowValuePrjCode.equalsIgnoreCase(prjCode) && rowValueCompSName.equalsIgnoreCase(contractorName)) {
                    String userId = settingsMatrix.getValue(i, 5);
                    prjUsers.add(userId);
                }
            }
        }
        return prjUsers;
    }
}
