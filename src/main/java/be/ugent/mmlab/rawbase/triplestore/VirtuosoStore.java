package be.ugent.mmlab.rawbase.triplestore;


import com.hp.hpl.jena.sparql.core.DatasetGraph;
import virtuoso.jena.driver.VirtDataset;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;

/**
 * @author Sam
 *
 */
public class VirtuosoStore {

    protected String virtURL;
    protected String user;
    protected String pass;
    protected String graphName = null;
    protected Boolean readFromAllGraphs = false;
    private VirtGraph graph;
    private VirtModel model;

    public VirtuosoStore() {

        virtURL = "jdbc:virtuoso://localhost:1111";
        user = "dba";
        pass = "dba";

    }

    public VirtuosoStore(String virtuosoURL, String username,
            String password, Boolean accessToAllGraphs) {

        virtURL = virtuosoURL;
        user = username;
        pass = password;
        readFromAllGraphs = accessToAllGraphs;

    }

    public VirtuosoStore(String virtuosoURL, String username,
            String password, String graph, Boolean accessToAllGraphs) {

        virtURL = virtuosoURL;
        user = username;
        pass = password;
        graphName = graph;
        readFromAllGraphs = accessToAllGraphs;
    }

    public VirtGraph getGraph() {

        if (graphName == null || graphName.isEmpty()) {

            graph = new VirtGraph(virtURL, user, pass);
            graph.setReadFromAllGraphs(readFromAllGraphs);
            return graph;

        } else {

            graph = new VirtGraph(graphName, virtURL, user, pass);
            graph.setReadFromAllGraphs(readFromAllGraphs);
            return graph;

        }

    }

    public void closeGraph() {

        graph.close();

    }

    public DatasetGraph getDatasetGraph() {

        VirtDataset ds = new VirtDataset(virtURL, user, pass);
        ds.setReadFromAllGraphs(readFromAllGraphs);
        return ds.asDatasetGraph();

    }

    public VirtModel getModel() {

        model = new VirtModel(getGraph());
        return model;

    }

    public void closeModel() {

        model.close();

    }
}
