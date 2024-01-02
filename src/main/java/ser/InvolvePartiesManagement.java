package ser;

import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import com.ser.foldermanager.IFolder;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InvolvePartiesManagement extends UnifiedAgent {
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

            if(Objects.equals(compIsMain, "1")){
                updateProjectCard(mainDocument);
            }

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void updateProjectCard(IDocument doc) throws Exception {
        try {
            IDocument prjCardDoc = getProjectCard(prjCode);
            if(prjCardDoc==null){
                throw new Exception("Exeption Caught..updateProjectCard..prjCardDoc is NULL");
            }

            if(doc.getDescriptorValue("ccmPRJCard_EngMng") != null) {
                prjCardDoc.setDescriptorValue("ccmPRJCard_EngMng", doc.getDescriptorValue("ccmPRJCard_EngMng"));
            }else{
                prjCardDoc.setDescriptorValue("ccmPRJCard_EngMng", "");
            }
            if(doc.getDescriptorValue("ccmPRJCard_prjmngr") != null) {
                prjCardDoc.setDescriptorValue("ccmPRJCard_prjmngr", doc.getDescriptorValue("ccmPRJCard_prjmngr"));
            }else{
                prjCardDoc.setDescriptorValue("ccmPRJCard_prjmngr", "");
            }
            if(doc.getDescriptorValue("ccmPrjCard_DccList") != null) {
                prjCardDoc.setDescriptorValues("ccmPrjCard_DccList", doc.getDescriptorValues("ccmPrjCard_DccList", String.class));
            }else{
                prjCardDoc.setDescriptorValue("ccmPrjCard_DccList", "");
            }
            if(doc.getDescriptorValue("ccmPrjCardUsers") != null) {
                prjCardDoc.setDescriptorValues("ccmPrjCardUsers", doc.getDescriptorValues("ccmPrjCardUsers", String.class));
            }else{
                prjCardDoc.setDescriptorValue("ccmPrjCardUsers", "");
            }
            prjCardDoc.commit();

        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardMembers2GVList: " + e);
        }
    }
    public void updateMembers2GVList(IDocument doc) throws Exception {
        try {
            String managerID = doc.getDescriptorValue("ccmPRJCard_EngMng");
            String managerName = getUserByWB(managerID);
            String pmanagerID = doc.getDescriptorValue("ccmPRJCard_prjmngr");
            String pmanagerName = getUserByWB(pmanagerID);
            String dccMembers = doc.getDescriptorValue("ccmPrjCard_DccList");
            String otherMembers = doc.getDescriptorValue("ccmPrjCardUsers");
            String ToReceiver = doc.getDescriptorValue("To-Receiver");
            String CcReceiver = doc.getDescriptorValue("CC-Receiver");
            String ObjectAuthors = doc.getDescriptorValue("ObjectAuthors");
            String membersD = "";
            String membersOth = "";
            String[] membersIDs = new String[0];
            String[] membersOthIDs = new String[0];

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
            List<String> memberOthList = new ArrayList<>();

            if(dccMembers != null) {
                membersD = doc.getDescriptorValue("ccmPrjCard_DccList").replace("[", "").replace("]", "");
                membersIDs = membersD.split(",");
            }
            if(otherMembers != null) {
                membersOth = doc.getDescriptorValue("ccmPrjCardUsers").replace("[", "").replace("]", "");
                membersOthIDs = membersOth.split(",");
            }

            for (String memberID : membersIDs) {
                String memberName = getUserByWB(memberID);
                memberList.add(memberName);
            }
            for (String memberOthID : membersOthIDs) {
                String memberLogin = getUserLoginByWB(memberOthID);
                memberOthList.add(memberLogin);
            }

            memberOthList.addAll(Arrays.asList(membersToReceiverIDs));
            memberOthList.addAll(Arrays.asList(membersCcReceiverIDs));
            memberOthList.addAll(Arrays.asList(membersOuthorIDs));

            boolean isRemove = removeByPrjCodeFromGVList();

            int rowCount = 0;
            if(!Objects.equals(managerName, "")) {
                IUser mmbr = getDocumentServer().getUserByLoginName(getSes() , getUserLoginByWB(managerID));
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                srtMatrixModify.appendRow();
                srtMatrixModify.commit();
                settingsMatrix.refresh();

                rowCount = settingsMatrix.getRowCount()-1;
                //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
                srtMatrixModify.setValue(rowCount, 0, prjCode, false);
                srtMatrixModify.setValue(rowCount, 1, compShortName, false);
                srtMatrixModify.setValue(rowCount, 2, compName, false);
                srtMatrixModify.setValue(rowCount, 3, mmbr.getFullName(), false);
                srtMatrixModify.setValue(rowCount, 4, mmbr.getLogin(), false);
                srtMatrixModify.setValue(rowCount, 5, mmbr.getID(), false);
                srtMatrixModify.setValue(rowCount, 6, "EM", false);
                srtMatrixModify.setValue(rowCount, 7, compIsMain, false);
                srtMatrixModify.commit();
                settingsMatrix.refresh();
            }

            if(!Objects.equals(pmanagerName, "")) {
                IUser mmbr = getDocumentServer().getUserByLoginName(getSes() , getUserLoginByWB(pmanagerID));
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                srtMatrixModify.appendRow();
                srtMatrixModify.commit();
                settingsMatrix.refresh();

                rowCount = settingsMatrix.getRowCount()-1;
                //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
                srtMatrixModify.setValue(rowCount, 0, prjCode, false);
                srtMatrixModify.setValue(rowCount, 1, compShortName, false);
                srtMatrixModify.setValue(rowCount, 2, compName, false);
                srtMatrixModify.setValue(rowCount, 3, mmbr.getFullName(), false);
                srtMatrixModify.setValue(rowCount, 4, mmbr.getLogin(), false);
                srtMatrixModify.setValue(rowCount, 5, mmbr.getID(), false);
                srtMatrixModify.setValue(rowCount, 6, "PM", false);
                srtMatrixModify.setValue(rowCount, 7, compIsMain, false);
                srtMatrixModify.commit();
                settingsMatrix.refresh();
            }

            if(!memberList.isEmpty()) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                int c = 0;
                for (String memberName : memberList) {
                    String mmbrID = membersIDs[c];
                    IUser mmbr = getDocumentServer().getUserByLoginName(getSes() , getUserLoginByWB(mmbrID));
                    srtMatrixModify.appendRow();
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    rowCount = settingsMatrix.getRowCount()-1;
                    srtMatrixModify.setValue(rowCount, 0, prjCode, false);
                    srtMatrixModify.setValue(rowCount, 1, compShortName, false);
                    srtMatrixModify.setValue(rowCount, 2, compName, false);
                    srtMatrixModify.setValue(rowCount, 3, mmbr.getFullName(), false);
                    srtMatrixModify.setValue(rowCount, 4, mmbr.getLogin(), false);
                    srtMatrixModify.setValue(rowCount, 5, mmbr.getID(), false);
                    srtMatrixModify.setValue(rowCount, 6, "DCC", false);
                    srtMatrixModify.setValue(rowCount, 7, compIsMain, false);
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    c++;
                }
            }

            if(!memberOthList.isEmpty()) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                int c = 0;
                for (String memberOthName : memberOthList) {
                    IUser mmbr = getDocumentServer().getUserByLoginName(getSes() , memberOthName);
                    srtMatrixModify.appendRow();
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    rowCount = settingsMatrix.getRowCount()-1;
                    srtMatrixModify.setValue(rowCount, 0, prjCode, false);
                    srtMatrixModify.setValue(rowCount, 1, compShortName, false);
                    srtMatrixModify.setValue(rowCount, 2, compName, false);
                    srtMatrixModify.setValue(rowCount, 3, mmbr.getFullName(), false);
                    srtMatrixModify.setValue(rowCount, 4, mmbr.getLogin(), false);
                    srtMatrixModify.setValue(rowCount, 5, mmbr.getID(), false);
                    srtMatrixModify.setValue(rowCount, 6, "OTHER", false);
                    srtMatrixModify.setValue(rowCount, 7, compIsMain, false);
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    c++;
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardMembers2GVList: " + e);
        }
    }
    public String getUserByWB(String wbID){
        String rtrn = "";
        if(wbID != null) {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowID = settingsMatrix.getValue(i, 0);
                if (rowID.equalsIgnoreCase(wbID)) {
                    rtrn = settingsMatrix.getValue(i, 2);
                    break;
                }
            }
        }
        return rtrn;
    }
    public String getUserLoginByWB(String wbID){
        String rtrn = "";
        if(wbID != null) {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowID = settingsMatrix.getValue(i, 0);
                if (rowID.equalsIgnoreCase(wbID)) {
                    rtrn = settingsMatrix.getValue(i, 1);
                    break;
                }
            }
        }
        return rtrn;
    }
    public void updateMembers2GVListOLD(IDocument doc) throws Exception {
        try {
            String ToReceiver = doc.getDescriptorValue("To-Receiver");
            String CcReceiver = doc.getDescriptorValue("CC-Receiver");
            String ObjectAuthors = doc.getDescriptorValue("ObjectAuthors");
            String isMyComp = doc.getDescriptorValue("ccmPRJCard_status");

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
                    srtMatrixModify.setValue(rowCount, 6, mmbr.getID(), false);
                    srtMatrixModify.setValue(rowCount, 7, mmbr.getID(), false);
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
            if (rowValuePrjCode.equals(prjCode) && rowValueCompSName.equals(compShortName)) {
                srtMatrixModify.removeRow(i);
                srtMatrixModify.commit();
                log.info("Removed Proje:" + rowValuePrjCode + " /// Comp:" + rowValueCompSName);
                if(removeByPrjCodeFromGVList()){break;}
            }
        }
        return true;
    }
    public void updateRole(List<String> members) throws Exception {
        try {
            String roleID = "";
            String roleIDDcc = "";
            if(Objects.equals(compIsMain, "1")){
                roleID = Conf.ClassIDs.InternalProjectUsers;
                roleIDDcc = Conf.ClassIDs.InternalDCC;
            }else{
                roleID = Conf.ClassIDs.ExternalProjectUsers;
                roleIDDcc = Conf.ClassIDs.ExternalDCC;
            }
            IRole prjRole = getSes().getDocumentServer().getRole(getSes(),roleID);
            IRole prjRoleDcc = getSes().getDocumentServer().getRole(getSes(),roleIDDcc);

            if(prjRole == null || prjRoleDcc == null){
                throw new Exception("Exeption Caught..updateRole..prjRole or RoleDcc is NULL");
            }

            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
            if(settingsMatrix!=null) {
                for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                    String userId = settingsMatrix.getValue(i, 5);
                    String role = settingsMatrix.getValue(i, 6);
                    IUser user = getDocumentServer().getUser(getSes() , userId);
                    if(user!=null) {
                        if(Objects.equals(role, "DCC")) {
                            addToRole(user, prjRoleDcc.getID());
                            addToRole(user, prjRole.getID());
                        }else{
                            addToRole(user, prjRole.getID());
                        }
                    }
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

            String unitName = (Objects.equals(compIsMain, "1") ? prjCode : prjCode + "_" + compShortName);
            IUnit unit = getDocumentServer().getUnitByName(getSes(), unitName);
            IUnit punit = getDocumentServer().getUnitByName(getSes(), (Objects.equals(compIsMain, "1") ? "Projects" : prjCode));
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
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        if(settingsMatrix!=null) {
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowValuePrjCode = settingsMatrix.getValue(i, 0);
                String rowValueCompSName = settingsMatrix.getValue(i, 1);
                if (rowValuePrjCode.equalsIgnoreCase(prjCode) && rowValueCompSName.equalsIgnoreCase(compShortName)) {
                    String userId = settingsMatrix.getValue(i, 5);
                    prjUsers.add(userId);
                }
            }
        }
        return prjUsers;
    }
    public IDocument getProjectCard(String prjNumber)  {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ProjectCard).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.PrjCardCode).append(" = '").append(prjNumber).append("'");
        String whereClause = builder.toString();
        System.out.println("Where Clause: " + whereClause);

        IInformationObject[] informationObjects = createQuery(new String[]{Conf.Databases.ProjectCard} , whereClause , 1);
        if(informationObjects.length < 1) {return null;}
        return (IDocument) informationObjects[0];
    }
    public IInformationObject[] createQuery(String[] dbNames , String whereClause , int maxHits){
        String[] databaseNames = dbNames;

        ISerClassFactory fac = getSrv().getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(
                getSes() ,
                databaseNames ,
                fac.getExpressionInstance(whereClause) ,
                null,null);
        if(maxHits > 0) {
            que.setMaxHits(maxHits);
            que.setHitLimit(maxHits + 1);
            que.setHitLimitThreshold(maxHits + 1);
        }
        IDocumentHitList hits = que.getSession() != null? que.getSession().getDocumentServer().query(que, que.getSession()):null;
        if(hits == null) return null;
        else return hits.getInformationObjects();
    }
}
