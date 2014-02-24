package be.ugent.mmlab.rawbase.jena;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.util.ModelUtils;
import java.sql.ResultSetMetaData;
import java.util.Iterator;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtResSetIter2;
import virtuoso.jena.driver.VirtuosoQueryExecution;

/**
 *
 * @author mielvandersande
 * @version 1.0
 *
 */
public class RawbaseQueryExecution extends VirtuosoQueryExecution {

    private Integer[] vPath; //CSV string serializing the path of deltas
    private RawbaseDataSet ds;
    
    private QuerySolution m_arg = null;

    public RawbaseQueryExecution(String query, VirtGraph _graph, Integer[] vPath) {
        this(query, _graph);
        this.vPath = vPath;
    }

    public RawbaseQueryExecution(String query, VirtGraph _graph) {
        super(query, _graph);
        this.ds = new RawbaseDataSet(ds);
    }

    /**
     * Turn the delta array into a fitting string
     * 
     * @return 
     */
    private String pathToString() {
        if (vPath.length == 0) {
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
    public Dataset getDataset() {
        return this.ds;
    }
    
    @Override
    public void setInitialBinding(QuerySolution arg)
    {
      this.m_arg = arg;
    }

    private String getVosQuery() {
        StringBuilder sb = new StringBuilder("");

        if (ds.getRuleSet() != null) {
            sb.append(" define input:inference '").append(ds.getRuleSet()).append("'\n");
        }

        if (ds.getSameAs()) {
            sb.append(" define input:same-as \"yes\"\n");
        }

        if (!ds.getReadFromAllGraphs()) {
            sb.append(" define input:default-graph-uri <").append(ds.getGraphName()).append("> \n");
        }

        String binded_query = substBindings(getQuery().toString());
        sb.append(binded_query);

        String query = "exec_versioned_sparql('" + sb.toString() + "', '" + pathToString() + "')";
        System.out.println("Query: " + query);
        return query;
    }
    
    private String substBindings(String query) 
    {
      if (m_arg == null)
        return query;

      StringBuilder buf = new StringBuilder();
      String delim = " ,)(;.";
      int i = 0;
      char ch;
      int qlen = query.length();
      while( i < qlen) {
        ch = query.charAt(i++);
        if (ch == '\\') {
	  buf.append(ch);
          if (i < qlen)
            buf.append(query.charAt(i++)); 

        } else if (ch == '"' || ch == '\'') {
          char end = ch;
      	  buf.append(ch);
      	  while (i < qlen) {
            ch = query.charAt(i++);
            buf.append(ch);
            if (ch == end)
              break;
      	  }
        } else  if ( ch == '?' ) {  //Parameter
      	  String varData = null;
      	  int j = i;
      	  while(j < qlen && delim.indexOf(query.charAt(j)) < 0) j++;
      	  if (j != i) {
            String varName = query.substring(i, j);
            RDFNode val = m_arg.get(varName);
            if (val != null) {
              varData = VirtGraph.Node2Str(val.asNode());
              i=j;
            }
          }
          if (varData != null)
            buf.append(varData);
          else
            buf.append(ch);
	} else {
      	  buf.append(ch);
    	}
      }
      return buf.toString();
    }

    public ResultSet execSelect(Integer[] vPath) {
        this.vPath = vPath;
        ResultSet ret = null;
        
        java.sql.Statement stmt = null;

        try {
            stmt = ds.createStatement();
            if (timeout > 0) {
                stmt.setQueryTimeout((int) (timeout / 1000));
            }
            java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());

            return new RResultSet(ds, rs);
        } catch (Exception e) {
            throw new JenaException("Can not create ResultSet.:" + e);
        }
    }
    
    @Override
    public Model execConstruct(Model model)
    {
        java.sql.Statement stmt = null;
        
      try {
        stmt = ds.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());
        ResultSetMetaData rsmd = rs.getMetaData();

        while(rs.next())
        {
          Node s = VirtGraph.Object2Node(rs.getObject(1));
          Node p = VirtGraph.Object2Node(rs.getObject(2));
          Node o = VirtGraph.Object2Node(rs.getObject(3));
          com.hp.hpl.jena.rdf.model.Statement st = ModelUtils.tripleToStatement(model, new Triple(s, p, o));
          if (st != null)
            model.add(st);
        }	
        rs.close();
        stmt.close();
        stmt = null;

      } catch (Exception e) {
        throw new JenaException("Convert results are FAILED.:"+e);
      }
      return model;
    }
    
    @Override
    public Iterator<Triple> execConstructTriples()
    {
        java.sql.Statement stmt = null;
      try {
        stmt = ds.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());
        return new VirtResSetIter2(ds, rs);

      } catch (Exception e) {
        throw new JenaException("execConstructTriples was FAILED.:"+e);
      }
    }
    
    @Override
     public Model execDescribe(Model model)
    {
        java.sql.Statement stmt = null;
      try {
        stmt = ds.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());
        ResultSetMetaData rsmd = rs.getMetaData();
        while(rs.next())
        {
          Node s = VirtGraph.Object2Node(rs.getObject(1));
          Node p = VirtGraph.Object2Node(rs.getObject(2));
          Node o = VirtGraph.Object2Node(rs.getObject(3));

          com.hp.hpl.jena.rdf.model.Statement st = ModelUtils.tripleToStatement(model, new Triple(s, p, o));
          if (st != null)
            model.add(st);
        }	
        rs.close();
        stmt.close();
        stmt = null;

      } catch (Exception e) {
        throw new JenaException("Convert results are FAILED.:"+e);
      }
      return model;
    }
     
     public Iterator<Triple> execDescribeTriples()
    {
        java.sql.Statement stmt = null;
      try {
        stmt = ds.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());
        return new VirtResSetIter2(ds, rs);

      } catch (Exception e) {
        throw new JenaException("execDescribeTriples was FAILED.:"+e);
      }
    }


    @Override
    public boolean execAsk() 
    {
      boolean ret = false;
      
      java.sql.Statement stmt = null;

      try {
        stmt = ds.createStatement();
        if (timeout > 0)
          stmt.setQueryTimeout((int)(timeout/1000));
        java.sql.ResultSet rs = stmt.executeQuery(getVosQuery());
        ResultSetMetaData rsmd = rs.getMetaData();

        while(rs.next())
        {
          if (rs.getInt(1) == 1)
            ret = true;
        }	
        rs.close();
        stmt.close();
        stmt = null;

      } catch (Exception e) {
        throw new JenaException("Convert results are FAILED.:"+e);
      }
      return ret;
    }

    //Extend inner class to provide access
    public class RResultSet extends VResultSet {

        public RResultSet(VirtGraph _g, java.sql.ResultSet _rs) {
            super(_g, _rs);
        }

    }
}
