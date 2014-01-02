/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena.vocabulary;

/**
 *
 * @author mielvandersande
 */
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class PROV {

    private static Model m_model = ModelFactory.createDefaultModel();
   
    public static String prefix = "prov";
    
    public static String ns = "http://www.w3.org/ns/prov#";
    
    public static final Property wasInfluencedBy        = m_model.createProperty(ns + "wasInfluencedBy");
    public static final Property wasDerivedFrom         = m_model.createProperty(ns + "wasDerivedFrom");
    public static final Property wasAttributedTo        = m_model.createProperty(ns + "wasAttributedTo");
    public static final Property atLocation             = m_model.createProperty(ns + "atLocation");
    public static final Property atTime                 = m_model.createProperty(ns + "atTime");
    public static final Property generated              = m_model.createProperty(ns + "generated");
    public static final Property used                   = m_model.createProperty(ns + "used");
    public static final Property wasAssociatedWith      = m_model.createProperty(ns + "wasAssociatedWith");
    public static final Property actedOnBehalfOf        = m_model.createProperty(ns + "actedOnBehalfOf");
    
    public static final Resource Person                 = m_model.createProperty(ns + "Person");
    public static final Resource Entity                 = m_model.createProperty(ns + "Entity");
    public static final Resource Activity               = m_model.createProperty(ns + "Activity");
}
