package ser;

import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import com.ser.foldermanager.IElements;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;
import com.ser.foldermanager.INodes;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class InvolvePartiesOnDelete extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    String prjCode = "";
    String compShortName = "";
    String compName = "";
    String paramName = "";
    String compIsMain = "";
    @Override
    protected Object execute() {
        IFolder mainFolder = null;
        try {
            mainFolder = getEventFolder();
            log.info("----OnDelete Contractor Started ---for mainFolder ID:--" + mainFolder.getID());
            paramName = "CCM_PARAM_CONTRACTOR-MEMBERS";
            IInformationObject[] projectCards = getProjectCards("Active");
            for(IInformationObject projectCard : projectCards){
                prjCode = projectCard.getDescriptorValue("ccmPRJCard_code");
                IInformationObject[] informationObjects = getInvolvePartiesFromNode((IFolder) projectCard, "Administration" ,"Involve Parties");
                if(informationObjects==null){
                    removeEntriesFromGVListByKey(prjCode);
                }else {
                    IInformationObject involveParty = informationObjects[0];
                    involveParty.setDescriptorValue("ccmPrjDocName", UUID.randomUUID().toString());
                    involveParty.commit();
                    log.info("----Contractor Updated Project Members GVList ---for (ID):" + involveParty.getID());
                }
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void removeEntriesFromGVListByKey(String keyval){
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());

        List<Integer> rows = new ArrayList<>();
        for(int i=0;i< settingsMatrix.getRowCount() ;i++){
            if(settingsMatrix.getRawValue(i,0).equals(keyval)) {
                String curVal = settingsMatrix.getRawValue(i,0);
                rows.add(i);
            }
        }
        for (int i = rows.size()-1  ; i >=0 ; i--) {
            srtMatrixModify.removeRow(rows.get(i));
        }
        srtMatrixModify.commit();
    }
    public IInformationObject[]  getInvolvePartiesFromNode(IFolder folder , String rootName, String nodeName) throws Exception {
        if(folder == null){
            throw new Exception("folder not found.");
        }
        List<INode> nodesByName = folder.getNodesByName(rootName);
        if(nodesByName.isEmpty()){
            throw new Exception(rootName + " Node not found.");
        }
        ISession ses = getSes();

        List<IInformationObject> elements = new ArrayList<>();

        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        INode newNode = root.getItemByName(nodeName);
        if(newNode != null) {
            log.info("Find Node : " + newNode.getID() + " /// " + nodeName);
            IElements nelements = newNode.getElements();
            for(int i=0;i<nelements.getCount2();i++) {
                elements.add(ses.getDocumentServer().getInformationObjectByID(nelements.getItem2(i).getLink(), ses));
            }
        }
        return elements.toArray(new IInformationObject[0]);
    }
    public IInformationObject[] getProjectCards(String status)  {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ProjectCard).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.PRJCard_status).append(" = '").append(status).append("'");
        String whereClause = builder.toString();
        System.out.println("Where Clause: " + whereClause);

        IInformationObject[] informationObjects = createQuery(new String[]{Conf.Databases.ProjectCard} , whereClause , 1000);
        if(informationObjects.length < 1) {return null;}
        return  informationObjects;
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
