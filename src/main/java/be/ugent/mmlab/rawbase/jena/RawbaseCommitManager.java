package be.ugent.mmlab.rawbase.jena;

import be.ugent.mmlab.virtuoso.jena.VirtDataSet;
import be.ugent.mmlab.virtuoso.jena.VirtGraph;
import be.ugent.mmlab.rawbase.jena.exceptions.RawbaseCommitException;
import be.ugent.mmlab.rawbase.jena.exceptions.RawbaseException;
import be.ugent.mmlab.rawbase.jena.vocabulary.PROV;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;

/**
 *
 * @author mielvandersande
 */
public class RawbaseCommitManager {

    // Constants used
    private boolean PERSISTANCE = false;
    //
    private RawbaseCommitIndex index;
    private VirtGraph graph;
    //Current, changeable commit
    private RawbaseCommit commit;
    //Singleton pattern
    private static RawbaseCommitManager instance;

    public enum Mode {
        ADD, DELETE, RESET
    };

    private RawbaseCommitManager() {
        this.index = new SimpleRawbaseVersionIndex();
    }

    public static RawbaseCommitManager getInstance() {
        if (instance == null) {
            instance = new RawbaseCommitManager();
        }
        return instance;
    }

    public void init(VirtGraph graph) throws RawbaseException {
        if (this.graph != null) {
            throw new RawbaseException("Index is already initialized");
        }

        this.graph = graph;
        syncIndex();
    }

    private VirtGraph getGraph() throws RawbaseException {
        if (this.graph == null) {
            throw new RawbaseException("Index is not yet initialized");
        }

        return this.graph;
    }

    public RawbaseCommitIndex getIndex() {
        return index;
    }

    public void syncIndex() throws RawbaseException {

        try {
            final String DELTAPARENT = "deltaparent", DELTAKEY = "deltakey", KEY = "key", PARENT = "parent";

            String sparql = "SELECT DISTINCT ?" + KEY + " ?" + PARENT + " ?" + DELTAKEY + " ?" + DELTAPARENT + " "
                    + "FROM <" + RawbaseCommit.PROV_GRAPHNAME.getURI() + ">"
                    + "FROM <" + RawbaseCommit.SYSTEM_GRAPHNAME.getURI() + ">"
                    + "WHERE { "
                    + "?" + KEY + " <" + RawbaseCommit.DELTA_KEY_PROPERTY.getURI() + "> ?" + DELTAKEY + " . "
                    + "OPTIONAL { "
                    + "?" + KEY + " <" + PROV.wasDerivedFrom.getURI() + "> ?" + PARENT + " . "
                    + "?" + PARENT + " <" + RawbaseCommit.DELTA_KEY_PROPERTY.getURI() + "> ?" + DELTAPARENT + " . "
                    + "} }";

            Statement st = getGraph().getConnection().createStatement();
            ResultSet results = st.executeQuery("sparql " + sparql);

            while (results.next()) {
                String key = results.getString(KEY);
                String parent = results.getString(PARENT);
                String deltakey = results.getString(DELTAKEY);
                String deltaparent = results.getString(DELTAPARENT);

                index.addDeltaMap(key, Integer.parseInt(deltakey));
                if (parent != null || deltaparent != null) {
                    index.addDeltaMap(parent, Integer.parseInt(deltaparent));
                    index.addCommit(key, parent);
                }

            }
        } catch (SQLException ex) {
            throw new RawbaseException(ex.getMessage());
        }
    }

    private boolean isInitialCommit() {
        return index.getCommits().isEmpty();
    }

    private RawbaseCommit retrieveCommit(String uri) throws RawbaseException {

        String sparql = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" + RawbaseCommit.PROV_GRAPHNAME + "> { <" + uri + "> ?p ?o}";

        QueryExecution qfac = QueryExecutionFactory.create(sparql, new VirtDataSet(getGraph()));

        Model m = qfac.execConstruct();

        ResIterator listSubjectsWithProperty = m.listSubjectsWithProperty(RDF.type, PROV.Activity);

        if (!listSubjectsWithProperty.hasNext()) {
            return null;
        }

        Resource subject = listSubjectsWithProperty.next();

        if (listSubjectsWithProperty.hasNext()) {
            throw new RawbaseCommitException("Model represents more than one commit");
        }

        String dtString = subject.getProperty(PROV.atTime).getString();
        DateTime dt = DateTime.parse(dtString);
        String message = subject.getProperty(DCTerms.description).getString();
        Resource author = subject.getPropertyResourceValue(PROV.wasAssociatedWith);

        Resource parentVersion = subject.getPropertyResourceValue(PROV.used);
        Resource version = subject.getPropertyResourceValue(PROV.generated);

        return new RawbaseCommitProxy(author, author, message, dt, parentVersion, getGraph());
    }

    public void startCommit(String author, String committer, String message, String parent) throws RawbaseException {

        if (commit != null) {
            throw new RawbaseCommitException("New commit cannot be started, commit in use.");
        }

        if (!isInitialCommit() && parent == null ) {
            throw new RawbaseCommitException("No parent commit given");
        }

        Resource authorRes = new ResourceImpl(author);
        Resource committerRes = new ResourceImpl(committer);

        this.commit = RawbaseCommitFactory.create(authorRes, committerRes, message, parent, PERSISTANCE, getGraph());
    }

    public void storeCommit() throws RawbaseException {
        if (this.commit == null) {
            return;
        }

        try {
            
            System.out.println("[RawbaseCommitManager]Starting Storing commit.");
            //Start the transaction -- DOES NOT WORK, check out later
            //getGraph().getConnection().setAutoCommit(false);

            //Store Addtions
            setDelta(Mode.ADD);
            System.out.print("[RawbaseCommitManager]Add " + commit.getAdditions().size() + " additions...");
            insertQuads(commit.getAdditions());
            System.out.print("done!" + System.lineSeparator());

            //Store Deletions
            setDelta(Mode.DELETE);
            System.out.print("[RawbaseCommitManager]Add " + commit.getDeletions().size() + " deletions...");
            insertQuads(commit.getDeletions());
            System.out.print("done!" + System.lineSeparator());

            //Reset the Table default delta value
            setDelta(Mode.RESET);

            //Store PROV
            System.out.print("[RawbaseCommitManager]Add PROV...");
            
            //Rawbase delta 
            insertQuads(commit.getPROV(index.nextDelta()));
            System.out.print("done!" + System.lineSeparator());

            System.out.print("[RawbaseCommitManager]Commit stored successfully! Syncing Index...");
            //Sync the index
            syncIndex();
            System.out.print("done!" + System.lineSeparator());
            this.commit = null;
        } catch (Exception ex) {
            //Manual rollback
            rollback();
            throw new RawbaseCommitException("Commit could not be stored: " + ex.getMessage());
        }

    }

    public void setDelta(Mode m) throws SQLException, RawbaseException {
        System.out.println("[RawbaseCommitManager]Switch the delta to " + m + "...");
        
        int delta = index.nextDelta();
        
        String sql = "ALTER TABLE RDF_QUAD MODIFY DA integer NOT NULL DEFAULT ";
        switch (m) {
            case ADD:
                sql += delta;
                break;
            case DELETE:
                sql += (delta + 1);
                break;
            case RESET:
                sql += "-1";
        }

        Statement s = getGraph().getConnection().createStatement();
        s.execute(sql);
 
        System.out.print("done!" + System.lineSeparator());
    }

    public void add(Quad q) {
        this.commit.addAddition(q);
    }

    public void delete(Quad q) throws RawbaseCommitException {
        if (isInitialCommit()){
            throw new RawbaseCommitException("Deleted triples are not allowed in the initial commit.");
        }
        this.commit.addDeletion(q);
    }

    private void rollback() throws RawbaseException {
        //Find out exactly what needs to be in this function
        //Manually rollback everything, if not possible with SQL transaction
        
        
    }

    public void discardCommit() {
        this.commit = null;
    }

    public void insertQuads(ArrayList<Quad> quads) throws SQLException, RawbaseException {
        HashMap<String, String> graphGroups = new HashMap<>();

        for (Quad quad : quads) {
            String g = quad.getGraph().getURI();
            String triples = graphGroups.get(g);

            if (triples == null) {
                graphGroups.put(g, RawbaseCommit.asNTriple(quad.asTriple()));
            } else {
                graphGroups.put(g, triples + RawbaseCommit.asNTriple(quad.asTriple()));
            }
        }

        java.sql.Statement st = getGraph().getConnection().createStatement();

        for (Map.Entry<String, String> entry : graphGroups.entrySet()) {
            String query = "sparql INSERT INTO <" + entry.getKey() + "> {" + entry.getValue() + "}";
            //System.out.println("[RawbaseCommitManager]Executing query: " + query);
            st.execute(query);
        }
    }



    private boolean isConflict(RawbaseCommit c) throws SQLException, RawbaseException {
        java.sql.Statement st = getGraph().getConnection().createStatement();
        String query = "sparql ASK"
                + "FROM <" + RawbaseCommit.PROV_GRAPHNAME.getURI() + ">"
                + "WHERE { "
                + "?x <" + PROV.wasDerivedFrom.getURI() + "> <" + c.getParent().getURI() + "> "
                + " }";
        ResultSet executeQuery = st.executeQuery(query);

        return executeQuery.getBoolean(1);
    }
}
