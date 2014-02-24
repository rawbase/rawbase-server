package be.ugent.mmlab.rawbase.jena;


import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.shared.JenaException;
import virtuoso.jena.driver.VirtDataset;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;

/**
 *
 * @author mielvandersande
 * @version 1.0
 *
 */
public class RawbaseQueryExecution extends VirtuosoQueryExecution {

    private Integer[] vPath; //CSV string serializing the path of deltas
    private VirtGraph graph;

    public RawbaseQueryExecution(String query, VirtGraph _graph, Integer[] vPath) {
        this(query, _graph);
        this.vPath = vPath;
    }

    public RawbaseQueryExecution(String query, VirtGraph _graph) {
        super(query, _graph);
        this.graph = _graph;
    }

    private String pathToString() {
        if (vPath.length == 0){
            return "";
        }
        
        StringBuilder sb = new StringBuilder("");
        for (int v : vPath) {
            sb.append(v).append(","); // no 0 expansion
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
    
    public Dataset getDataset()
    {
      return new RawbaseDataSet(graph);
    }

    private String getVosQuery() {
        StringBuilder sb = new StringBuilder("");

        if (getGraph().getRuleSet() != null) {
            sb.append(" define input:inference '").append(getGraph().getRuleSet()).append("'\n");
        }

        if (getGraph().getSameAs()) {
            sb.append(" define input:same-as \"yes\"\n");
        }

        if (!getGraph().getReadFromAllGraphs()) {
            sb.append(" define input:default-graph-uri <").append(getGraph().getGraphName()).append("> \n");
        }

        String binded_query = substBindings(virt_query);
        sb.append(binded_query);

        String query = "exec_versioned_sparql('" + sb.toString() + "', '" + pathToString() + "')";
        System.out.println("Query: " + query);
        return query;
    }
    
    

    public ResultSet execSelect(Integer[] vPath) {
        this.vPath = vPath;
        ResultSet ret = null;

      try {
        stmt = graph.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());

        return new RResultSet(graph, rs);
      }	catch(Exception e) {
        throw new JenaException("Can not create ResultSet.:"+e);
      }
    }
    
    
    //Extend inner class to provide access
    public class RResultSet extends VResultSet{

        public RResultSet(VirtGraph _g, java.sql.ResultSet _rs) {
            super(_g, _rs);
        }

    }
}
