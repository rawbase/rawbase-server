package be.ugent.mmlab.triplestore;

import be.ugent.mmlab.jena.rawbase.RawbaseCommitManager;
import be.ugent.mmlab.jena.rawbase.RawbaseDataSet;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.fuseki.Fuseki;

/**
 * @author Sam
 *
 */
public class RawbaseStore extends VirtuosoStore {

    public RawbaseStore() {
        super();
        initIndex();
    }

    public RawbaseStore(String virtuosoURL, String username, String password, Boolean accessToAllGraphs) {
        super(virtuosoURL, username, password, accessToAllGraphs);
        initIndex();
    }

    public RawbaseStore(String virtuosoURL, String username, String password, String graph, Boolean accessToAllGraphs) {
        super(virtuosoURL, username, password, graph, accessToAllGraphs);
        initIndex();
    }
    
    private void initIndex(){
        Fuseki.configLog.info("Initializing the version index...");
        try {
            RawbaseCommitManager.getInstance().init(getGraph());
        } catch (Exception ex) {
            Logger.getLogger(RawbaseStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        Fuseki.configLog.info("Version index initialized!");
    }
    
    @Override
    public DatasetGraph getDatasetGraph() {
        RawbaseDataSet ds = new RawbaseDataSet(virtURL, user, pass);
        ds.setReadFromAllGraphs(readFromAllGraphs);
        return ds.asDatasetGraph();
    }


    
    
}
