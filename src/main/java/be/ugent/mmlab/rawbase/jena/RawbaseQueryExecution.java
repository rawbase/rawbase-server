package be.ugent.mmlab.rawbase.jena;

import be.ugent.mmlab.virtuoso.jena.VirtGraph;
import be.ugent.mmlab.virtuoso.jena.VirtuosoQueryExecution;
import com.hp.hpl.jena.query.ResultSet;

/**
 *
 * @author mielvandersande
 * @version 1.0
 *
 */
public class RawbaseQueryExecution extends VirtuosoQueryExecution {

    private Integer[] vPath; //CSV string serializing the path of deltas

    public RawbaseQueryExecution(String query, VirtGraph _graph, Integer[] vPath) {
        super(query, _graph);
        this.vPath = vPath;
    }

    public RawbaseQueryExecution(String query, VirtGraph _graph) {
        super(query, _graph);
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

    @Override
    public String getVirtQueryString() {
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
        return super.execSelect();
    }
}
