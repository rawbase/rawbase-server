/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;


import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author mielvandersande
 */
public class RawbaseCommitProxy extends RawbaseCommit {
    
    private VirtGraph graph;
    

    public RawbaseCommitProxy(Resource author, Resource committer, String message, VirtGraph graph) {
        this(author, committer, message, new DateTime(), null);
    }

    public RawbaseCommitProxy(Resource author, Resource committer, String message, Resource parent, VirtGraph graph) {
        this(author, committer, message, new DateTime(), parent, graph);
    }

    public RawbaseCommitProxy(Resource author, Resource committer, String message, DateTime datetime, VirtGraph graph) {
        this(author, committer, message, datetime, null, graph);
    }

    public RawbaseCommitProxy(Resource author, Resource committer, String message, DateTime datetime, Resource parent, VirtGraph graph) {
        super(author, committer, message, datetime, parent);
        this.graph = graph;
    }

    public VirtGraph getGraph() {
        return graph;
    }
    
    @Override
    ArrayList<Quad> getAdditions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    ArrayList<Quad> getDeletions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    void addAddition(Quad q) {
        try {
            String query = "sparql INSERT INTO <" + q.getGraph().getURI() + "> {" + asNTriple(q.getSubject(), q.getPredicate(), q.getObject()) + "}";
            java.sql.Statement st = getGraph().getConnection().createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            Logger.getLogger(RawbaseCommitProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    void addDeletion(Quad q) {
        try {
            String query = "sparql INSERT INTO <" + q.getGraph().getURI() + "> {" + asNTriple(q.getSubject(), q.getPredicate(), q.getObject()) + "}";
            java.sql.Statement st = getGraph().getConnection().createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            Logger.getLogger(RawbaseCommitProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
}
