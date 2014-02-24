/*
 *  $Id$
 *
 *  This file is part of the OpenLink Software Virtuoso Open-Source (VOS)
 *  project.
 *
 *  Copyright (C) 1998-2013 OpenLink Software
 *
 *  This project is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation; only version 2 of the License, dated June 1991.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package be.ugent.mmlab.rawbase.jena;

import be.ugent.mmlab.rawbase.jena.exceptions.RawbaseCommitException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.LabelExistsException;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.sql.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;
import virtuoso.jdbc4.VirtuosoDataSource;
import virtuoso.jena.driver.VirtDataset;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtResSetQIter;

//http://www.openlinksw.com/schemas/virtrdf#
//http://localhost:8890/DAV
//http://www.w3.org/2002/07/owl#
public class RawbaseDataSet extends VirtGraph implements Dataset {

    /**
     * Default model - may be null - according to Javadoc
     */
    Model defaultModel = null;
    private final Context m_context = new Context();

    private final Lock lock = new LockMRSW();

    public RawbaseDataSet(String _graphName, VirtuosoDataSource _ds) {
        super(_graphName, _ds);
    }

    public RawbaseDataSet(VirtGraph g) {
        super(g.getDataSource());
        //super(user, null);
        //this.graphName = g.getGraphName();
        
        //setReadFromAllGraphs(g.getReadFromAllGraphs());
        //this.url_hostlist = g.getGraphUrl();
        //this.user = g.getGraphUser();
        //this.password = g.getGraphPassword();
        //this.roundrobin = false;
        //setFetchSize(g.getFetchSize());
        //this.connection = g.getConnection();

    }

    public RawbaseDataSet(String url_hostlist, String user, String password) {
        super(url_hostlist, user, password);
    }

    /**
     * Get the default graph as a Jena Model
     */
    public Model getDefaultModel() {
        return defaultModel;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return super.createStatement(); //To change body of generated methods, choose Tools | Templates.
    }
    

    /**
     * Set the background graph. Can be set to null for none.
     */
    @Override
    public void setDefaultModel(Model model) {
        if (!(model instanceof VirtDataset)) {
            throw new IllegalArgumentException("VirtDataSource supports only VirtModel as default model");
        }
        defaultModel = model;
    }

    /**
     * Get a graph by name as a Jena Model
     */
    @Override
    public Model getNamedModel(String name) {
        try {
            DataSource _ds = getDataSource();
            if (_ds != null) {
                return new VirtModel(new VirtGraph(name, _ds));
            } else {
                return new VirtModel(new VirtGraph(name, this.getGraphUrl(),
                        this.getGraphUser(), this.getGraphPassword()));
            }
        } catch (Exception e) {
            throw new JenaException(e);
        }
    }

    /**
     * Does the dataset contain a model with the name supplied?
     *
     * @param name
     * @return
     */
    @Override
    public boolean containsNamedModel(String name) {
        String query = "SELECT TOP 1 * FROM RDF_QUAD WHERE G = iri_to_id(?)";
        ResultSet rs = null;

        checkOpen();
        try {
            java.sql.PreparedStatement ps = prepareStatement(query);
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }
            rs.close();
        } catch (Exception e) {
            throw new JenaException(e);
        }
        return false;
    }

    /**
     * Set a named graph.
     */
    public void addNamedModel(String name, Model model, boolean checkExists) throws LabelExistsException {

        //  try {
        if (checkExists && containsNamedModel(name)) {
            throw new LabelExistsException("A model with ID '" + name
                    + "' already exists.");
        }
        Graph g = model.getGraph();
        add(name, g.find(Node.ANY, Node.ANY, Node.ANY), null);
        /*  int count = 0;
         java.sql.PreparedStatement ps = prepareStatement(sinsert);

         for (Iterator i = g.find(Node.ANY, Node.ANY, Node.ANY); i.hasNext();) {
         Triple t = (Triple) i.next();

         ps.setString(1, name);
         bindSubject(ps, 2, t.getSubject());
         bindPredicate(ps, 3, t.getPredicate());
         bindObject(ps, 4, t.getObject());
         ps.addBatch();
         count++;
         if (count > BATCH_SIZE) {
         ps.executeBatch();
         ps.clearBatch();
         count = 0;
         }
         }
         if (count > 0) {
         ps.executeBatch();
         ps.clearBatch();
         }
         } catch (Exception e) {
         throw new JenaException(e);
         }*/
    }

    /**
     * Set a named graph.
     */
    @Override
    public void addNamedModel(String name, Model model) throws LabelExistsException {
        addNamedModel(name, model, true);
    }

    /**
     * Remove a named graph.
     */
    @Override
    public void removeNamedModel(String name) {
        String exec_text = "sparql clear graph <" + name + ">";

        checkOpen();
        try {
            java.sql.Statement stmt = createStatement();
            stmt.executeQuery(exec_text);
            stmt.close();
        } catch (Exception e) {
            throw new JenaException(e);
        }
    }

    /**
     * Change a named graph for another using the same name
     */
    @Override
    public void replaceNamedModel(String name, Model model) {
        try {
            removeNamedModel(name);
            addNamedModel(name, model);
            getConnection().commit();
            getConnection().setAutoCommit(true);
        } catch (Exception e) {
            throw new JenaException("Could not replace model:", e);
        }
    }

    /**
     * List the names
     *
     * @return
     */
    @Override
    public Iterator<String> listNames() {
        String exec_text = "DB.DBA.SPARQL_SELECT_KNOWN_GRAPHS()";
        ResultSet rs = null;
        int ret = 0;

        checkOpen();
        try {
            List<String> names = new LinkedList();

            java.sql.Statement stmt = createStatement();
            rs = stmt.executeQuery(exec_text);
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            return names.iterator();
        } catch (Exception e) {
            throw new JenaException(e);
        }
    }

    /**
     * Get the lock for this dataset
     */
    @Override
    public Lock getLock() {
        return lock;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#getContext()
     */
    @Override
    public Context getContext() {
        return m_context;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#supportsTransactions()
     */
    @Override
    public boolean supportsTransactions() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#begin(com.hp.hpl.jena.query.ReadWrite)
     */
    @Override
    public void begin(ReadWrite readWrite) {
        // TODO Auto-generated method stub
        //this.getTransactionHandler().begin();
        try {
            getConnection().setAutoCommit(false);
        } catch (Exception e) {
        }
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#commit()
     */
    @Override
    public void commit() {
        // TODO Auto-generated method stub
        //this.getTransactionHandler().commit();
        try {
            getConnection().commit();
        } catch (Exception e) {
        }
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#abort()
     */
    @Override
    public void abort() {

        //this.getTransactionHandler().abort();
        try {
            getConnection().rollback();
        } catch (Exception e) {
        }
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#isInTransaction()
     */
    @Override
    public boolean isInTransaction() {
        // TODO Auto-generated method stub
        //return false;
        try {
            return (!(getConnection().getAutoCommit()));
        } catch (Exception e) {
            return false;
        }
    }


    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.Dataset#end()
     */
    @Override
    public void end() {
        try {
//      getConnection().commit();
            getConnection().rollback();
            getConnection().setAutoCommit(true);
        } catch (Exception e) {
        }
    }

    /**
     * Get the dataset in graph form
     */
    @Override
    public DatasetGraph asDatasetGraph() {
        return new RawbaseDataSetGraph(this);
    }

    private void add(String name, ExtendedIterator<Triple> it, Object object) {
        while (it.hasNext()) {
            Triple t = (Triple) it.next();
            ResourceImpl g = new ResourceImpl(name);
            RawbaseCommitManager.getInstance().add(new Quad(g.asNode(), t));
        }
    }

    public class RawbaseDataSetGraph implements DatasetGraph {

        RawbaseDataSet vd = null;

        public RawbaseDataSetGraph(RawbaseDataSet vds) {
            vd = vds;
        }

        @Override
        public Graph getDefaultGraph() {
            return vd;
        }

        @Override
        public Graph getGraph(Node graphNode) {
            try {
                return new VirtGraph(graphNode.toString(), vd.getGraphUrl(),
                        vd.getGraphUser(), vd.getGraphPassword());
            } catch (Exception e) {
                throw new JenaException(e);
            }
        }

        @Override
        public boolean containsGraph(Node graphNode) {
            return containsNamedModel(graphNode.toString());
        }

        protected List<Node> getListGraphNodes() {
            String exec_text = "DB.DBA.SPARQL_SELECT_KNOWN_GRAPHS()";
            ResultSet rs = null;
            int ret = 0;

            vd.checkOpen();
            try {
                List<Node> names = new LinkedList();

                java.sql.Statement stmt = vd.createStatement();
                rs = stmt.executeQuery(exec_text);
                while (rs.next()) {
                    names.add(Node.createURI(rs.getString(1)));
                }
                return names;
            } catch (Exception e) {
                throw new JenaException(e);
            }
        }

        @Override
        public Iterator<Node> listGraphNodes() {
            return getListGraphNodes().iterator();
        }

        @Override
        public Lock getLock() {
            return vd.getLock();
        }

        @Override
        public long size() {
            return vd.size();
        }

        @Override
        public void close() {
            vd.close();
        }

        @Override
        public Context getContext() {
            return vd.m_context;
        }

        @Override
        public void setDefaultGraph(Graph g) {
            //Code by SAM
            try {
                getConnection().setAutoCommit(false);
                setDefaultModel(ModelFactory.createModelForGraph(g));
                getConnection().commit();
                getConnection().setAutoCommit(true);
            } catch (Exception e) {
                try {
                    getConnection().rollback();
                } catch (Exception e2) {
                    throw new JenaException(
                            "Could not set the default model, and could not rollback!", e2);
                }
                throw new JenaException("Could not set the default model:", e);
            }
            //New Code by Jena Driver
            /*if (!(g instanceof RawbaseDataSet1))
             throw new IllegalArgumentException("VirtDataSetGraph.setDefaultGraph() supports only VirtGraph as default graph");

             vd = new RawbaseDataSet1((RawbaseDataSet1)g);*/
        }

        /**
         * Add the given graph to the dataset.
         * <em>Replaces</em> any existing data for the named graph; to add data,
         * get the graph and add triples to it, or add quads to the dataset. Do
         * not assume that the same Java object is returned by {@link #getGraph}
         */
        public void addGraph(Node graphName, Graph graph) {
            //SAM
            try {
                getConnection().setAutoCommit(false);
                addNamedModel(graphName.toString(), ModelFactory.createModelForGraph(graph));
                getConnection().commit();
                getConnection().setAutoCommit(true);
            } catch (Exception e) {
                try {
                    getConnection().rollback();
                } catch (Exception e2) {
                    throw new JenaException(
                            "Could not add the named model, and could not rollback!", e2);
                }
                throw new JenaException("Could not add the named model:", e);
            }

            //New code by Jena Driver
            /*try {
             vd.clearGraph(graphName.toString());
             //??todo add optimize  when graph is VirtGraph
             ExtendedIterator<Triple> it = graph.find(Node.ANY, Node.ANY, Node.ANY);
             vd.add(graphName.toString(), it, null);
             } catch (Exception e) {
             throw new JenaException("Error in addGraph:"+e);
             }*/
        }

        public void removeGraph(Node graphName) {
            //SAM
            try {
                getConnection().setAutoCommit(false);
                removeNamedModel(graphName.toString());
                getConnection().commit();
                getConnection().setAutoCommit(true);
            } catch (Exception e) {
                try {
                    getConnection().rollback();
                } catch (Exception e2) {
                    throw new JenaException(
                            "Could not remove the named model, and could not rollback!", e2);
                }
                throw new JenaException("Could not remove the named model:", e);
            }

            //New code by Jena Driver
            /*try {
             vd.clearGraph(graphName.toString());
             } catch (Exception e) {
             throw new JenaException("Error in removeGraph:"+e);
             }*/
        }

        @Override
        public void add(Quad quad) {
            //add(quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
            RawbaseCommitManager.getInstance().add(quad);
        }

        @Override
        public void delete(Quad quad) {
            try {
                //delete(quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
                RawbaseCommitManager.getInstance().delete(quad);
            } catch (RawbaseCommitException ex) {
                throw new JenaException("Illegal delete:", ex);
            }
        }

        public void add(Node g, Node s, Node p, Node o) {
            add(new Quad(g, s, p, o));

        }

        public void delete(Node g, Node s, Node p, Node o) {
            delete(new Quad(g, s, p, o));

        }

        public void deleteAny(Node g, Node s, Node p, Node o) {
            //SAM
            vd.checkOpen();
            ExtendedIterator<Triple> tripleIt = vd.find(s, p, o);
            while (tripleIt.hasNext()) {
                Triple q = tripleIt.next();
                delete(g, q.getSubject(), q.getPredicate(), q.getObject());
            }

        }

        public Iterator<Quad> find() {
            //SAM
            //vd.checkOpen();
            //return triples2quads(null, vd.find(null, null, null));
            return find(Node.ANY, Node.ANY, Node.ANY, Node.ANY);
        }

        public Iterator<Quad> find(Quad quad) {
            return find(quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        }

        public Iterator<Quad> find(Node g, Node s, Node p, Node o) {
            List<Node> graphs;
            if (isWildcard(g)) {
                graphs = getListGraphNodes();
            } else {
                graphs = new LinkedList();
                graphs.add(g);
            }

            return new VirtResSetQIter(vd, graphs.iterator(), new Triple(s, p, o));
        }

        /**
         * Find matching quads in the dataset in named graphs only - may include
         * wildcards, Node.ANY or null
         *
         * @see Graph#find(Node,Node,Node)
         */
        @Override
        public Iterator<Quad> findNG(Node g, Node s, Node p, Node o) {
            return find(g, s, p, o);
        }

        /**
         * Test whether the dataset (including default graph) contains a quad -
         * may include wildcards, Node.ANY or null
         */
        public boolean contains(Node g, Node s, Node p, Node o) {
            if (isWildcard(g)) {
                boolean save = vd.getReadFromAllGraphs();
                vd.setReadFromAllGraphs(true);
                boolean ret = vd.graphBaseContains(null, new Triple(s, p, o));
                vd.setReadFromAllGraphs(save);
                return ret;
            } else {
                return vd.graphBaseContains(g.toString(), new Triple(s, p, o));
            }
        }

        /**
         * Test whether the dataset contains a quad (including default graph)-
         * may include wildcards, Node.ANY or null
         */
        public boolean contains(Quad quad) {
            return contains(quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        }

        public boolean isEmpty() {
            return vd.isEmpty();
        }

        protected boolean isWildcard(Node g) {
            return g == null || Node.ANY.equals(g);
        }

    }
}
