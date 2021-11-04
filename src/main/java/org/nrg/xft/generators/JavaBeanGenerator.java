/*
 * core: org.nrg.xft.generators.JavaBeanGenerator
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */


package org.nrg.xft.generators;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.meta.XFTMetaManager;
import org.nrg.xft.references.XFTRelationSpecification;
import org.nrg.xft.references.XFTSuperiorReference;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperElement;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperFactory;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperField;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.utils.XftStringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Slf4j
public class JavaBeanGenerator {
    public static final String INTERFACE_PACKAGE = "org.nrg.xdat.model";
    private             String project           = "";

    private enum TYPE {data, single_reference, multi_reference, inline_repeater, LONG_DATA, NO_CHILD}

    /**
     * @param element  The element
     * @param location The location where the Java bean file should be written.
     *
     * @throws Exception When something goes wrong.
     */
    public void generateJavaBeanFile(GenericWrapperElement element, String location) throws Exception {

        StringBuffer sb = new StringBuffer();

        String packageName = project + ".bean";

        sb.append("/*\n * GENERATED FILE\n * Created on ").append(Calendar.getInstance().getTime()).append("\n * JavaBeanGenerator.generateJavaBeanFile(GenericWrapperElement, String)\n */");
        sb.append("\npackage ").append(packageName).append(";\n\n");
        //IMPORTS
        sb.append("import com.fasterxml.jackson.annotation.JsonIgnore;\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        sb.append("import org.apache.log4j.Logger;\n");
        sb.append("import org.nrg.xdat.bean.base.BaseElement;\n\n");
        sb.append("import javax.xml.bind.annotation.XmlRootElement;\n");
        sb.append("import javax.xml.bind.annotation.XmlTransient;\n");
        sb.append("import java.util.*;\n\n");
        sb.append("\n\n/**\n * @author XDAT\n *\n */");

        //CLASS
        String extensionName = "BaseElement";
        if (element.isExtension()) {
            GenericWrapperElement ext = GenericWrapperElement.GetElement(element.getExtensionType());
            if (!ext.getXSIType().equals(element.getXSIType())) {
                extensionName = getFormattedBean(ext);
            }
        }


        sb.append("/*\n ******************************** \n * DO NOT MODIFY THIS FILE \n *\n ********************************/");

        sb.append("\n@SuppressWarnings({\"unchecked\",\"rawtypes\"})\n");
        sb.append("@XmlRootElement");
        sb.append("\npublic class ").append(getFormattedBean(element)).append(" extends ").append(extensionName).append(" implements java.io.Serializable, ").append(INTERFACE_PACKAGE).append(".").append(getFormattedInterface(element)).append(" {");
        sb.append("\n\tpublic static final Logger logger = Logger.getLogger(").append(getFormattedBean(element)).append(".class);");
        sb.append("\n\tpublic static final String SCHEMA_ELEMENT_NAME=\"").append(element.getFullXMLName()).append("\";");
        //ADD CONSTRUCTORS


        //ADD SCHEMA METHODS
        sb.append("\n\n");
        sb.append("\t").append("public String getSchemaElementName(){");
        sb.append("\n\t\t").append("return \"").append(element.getLocalXMLName()).append("\";");
        sb.append("\n\t}");
        sb.append("\n\n");
        sb.append("\t").append("public String getFullSchemaElementName(){");
        sb.append("\n\t\t").append("return \"").append(element.getFullXMLName()).append("\";");
        sb.append("\n\t}");

        ArrayList<String> xmlPaths        = new ArrayList<>();
        ArrayList<TYPE>   types           = new ArrayList<>();
        ArrayList<String> methods         = new ArrayList<>();
        ArrayList<String> gets            = new ArrayList<>();
        ArrayList<String> foreignElements = new ArrayList<>();

        for (final Object o : element.getAllFields(true, true)) {
            GenericWrapperField f = (GenericWrapperField) o;
            if (f.isReference()) {
                String xmlPath   = f.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                if (formatted.equalsIgnoreCase("USER")) {
                    formatted = "userProperty";
                }
                SchemaElementI foreign = f.getReferenceElement();

                if (foreign.getGenericXFTElement().getAddin().equals("")) {
                    final String foreignClassName = getFormattedBean(foreign.getGenericXFTElement());
                    final String foreignInterface = getFormattedInterface(foreign.getGenericXFTElement());

                    if (f.isMultiple()) {
                        xmlPaths.add(f.getXMLPathString());
                        if (f.isInLineRepeaterElement()) {
                            types.add(TYPE.inline_repeater);
                        } else if (foreign.getGenericXFTElement().isANoChildElement()) {
                            types.add(TYPE.NO_CHILD);
                        } else {
                            types.add(TYPE.multi_reference);
                        }
                        methods.add("add" + formatted);
                        gets.add("get" + formatted);
                        foreignElements.add(foreign.getGenericXFTElement().getSchemaTargetNamespaceURI() + ":" + foreign.getGenericXFTElement().getLocalXMLName());

                        sb.append("\n\t private List");
                        sb.append("<").append(project).append(".bean.").append(foreignClassName).append(">");
                        sb.append(" _").append(formatted).append(" =new ArrayList");
                        sb.append("<").append(project).append(".bean.").append(foreignClassName).append(">");
                        sb.append("();");
                        sb.append("\n");
                        sb.append("\n\t/**");
                        sb.append("\n\t * ").append(xmlPath);
                        sb.append("\n\t * @return Returns an List of ").append(project).append(".bean.").append(foreignClassName).append("\n\t */");
                        sb.append("\n\t").append("public ");
                        sb.append("<A extends " + INTERFACE_PACKAGE + ".").append(foreignInterface).append("> List<A>");
                        sb.append(" get").append(formatted).append("() {");
                        sb.append("\n\t\treturn (List<A>) _").append(formatted).append(";");
                        sb.append("\n\t}");

                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void set").append(formatted).append("(ArrayList");
                        sb.append("<").append(project).append(".bean.").append(foreignClassName).append(">");
                        sb.append(" v){");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Adds the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void add").append(formatted).append("(").append(project).append(".bean.").append(foreignClassName).append(" v){");
                        sb.append("\n\t\t_").append(formatted).append(".add(v);");
                        sb.append("\n\t}");

                        sb.append("\n");
                        sb.append("\n\t/**");
                        sb.append("\n\t * ").append(xmlPath);
                        sb.append("\n\t * Adds org.nrg.xdat.model.").append(foreignInterface).append("\n\t */");
                        sb.append("\n\t").append("public <A extends ").append(INTERFACE_PACKAGE + ".").append(foreignInterface).append("> void add").append(formatted).append("(A item) throws Exception{");
                        sb.append("\n\t_").append(formatted).append(".add").append("((").append(project).append(".bean.").append(foreignClassName).append(")item);");
                        sb.append("\n\t}");

                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Adds the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void add").append(formatted).append("(Object v){");
                        sb.append("\n\t\tif (v instanceof ").append(project).append(".bean.").append(foreignClassName).append(")");
                        sb.append("\n\t\t\t_").append(formatted).append(".add((").append(project).append(".bean.").append(foreignClassName).append(")v);");
                        sb.append("\n\t\telse");
                        sb.append("\n\t\t\tthrow new IllegalArgumentException(\"Must be a valid ").append(project).append(".bean.").append(foreignClassName).append("\");");
                        sb.append("\n\t}");


                    } else {
                        if (!f.getName().equalsIgnoreCase(element.getExtensionFieldName())) {
                            xmlPaths.add(f.getXMLPathString());
                            if (f.isInLineRepeaterElement()) {
                                types.add(TYPE.inline_repeater);
                            } else if (foreign.getGenericXFTElement().isANoChildElement()) {
                                types.add(TYPE.NO_CHILD);
                            } else {
                                types.add(TYPE.single_reference);
                            }
                            methods.add("set" + formatted);
                            gets.add("get" + formatted);
                            foreignElements.add(foreign.getGenericXFTElement().getSchemaTargetNamespaceURI() + ":" + foreign.getGenericXFTElement().getLocalXMLName());
                            sb.append("\n\t private ").append(project).append(".bean.").append(foreignClassName).append(" _").append(formatted).append(" =null;");
                            sb.append("\n");
                            sb.append("\n\t/**");
                            sb.append("\n\t * ").append(xmlPath);
                            sb.append("\n\t * @return ").append(project).append(".bean.").append(foreignClassName).append("\n\t */");
                            sb.append("\n\t").append("public ").append(project).append(".bean.").append(foreignClassName).append(" get").append(formatted).append("() {");
                            sb.append("\n\t\treturn _").append(formatted).append(";");
                            sb.append("\n\t}");

                            sb.append("\n\n");
                            sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                            sb.append("\n\t").append("@JsonProperty");
                            sb.append("\n\t").append("public void set").append(formatted).append("(").append(project).append(".bean.").append(foreignClassName).append(" v){");
                            sb.append("\n\t\t_").append(formatted).append(" =v;");
                            sb.append("\n\t}");

                            sb.append("\n\n");
                            sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                            sb.append("\n\t").append("@XmlTransient");
                            sb.append("\n\t").append("@JsonIgnore");
                            sb.append("\n\t").append("public void set").append(formatted).append("(Object v) {");
                            sb.append("\n\t\tif (v instanceof ").append(project).append(".bean.").append(foreignClassName).append(")");
                            sb.append("\n\t\t\t_").append(formatted).append(" =(").append(project).append(".bean.").append(foreignClassName).append(")v;");
                            sb.append("\n\t\telse");
                            sb.append("\n\t\t\tthrow new IllegalArgumentException(\"Must be a valid ").append(project).append(".bean.").append(foreignClassName).append("\");");
                            sb.append("\n\t}");

                            sb.append("\n");
                            sb.append("\n\t/**");
                            sb.append("\n\t * ").append(xmlPath);
                            sb.append("\n\t * @return ").append(INTERFACE_PACKAGE + ".").append(foreignInterface).append("\n\t */");
                            sb.append("\n\t").append("@XmlTransient");
                            sb.append("\n\t").append("@JsonIgnore");
                            sb.append("\n\t").append("public <A extends ").append(INTERFACE_PACKAGE + ".").append(foreignInterface).append("> void set").append(formatted).append("(A item) throws Exception{");
                            sb.append("\n\t").append("set").append(formatted).append("((").append(project).append(".bean.").append(foreignClassName).append(")item);");
                            sb.append("\n\t}");


                            final XFTSuperiorReference ref = (XFTSuperiorReference) f.getXFTReference();
                            for (final XFTRelationSpecification spec : ref.getKeyRelations()) {
                                String temp = spec.getLocalCol();

                                final String type = spec.getSchemaType().getLocalType();
                                if (type != null) {
                                    if (type.equalsIgnoreCase("integer")) {
                                        String reformatted = formatted + "FK";
                                        sb.append("\n\n\t").append("//FIELD");
                                        sb.append("\n\n\t").append("private Integer _").append(reformatted);
                                        sb.append("=null;");

                                        //STANDARD GET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sb.append("\n\t").append("public Integer get").append(reformatted).append("(){");
                                        sb.append("\n\t\treturn _").append(reformatted).append(";");
                                        sb.append("\n\t}");

                                        //STANDARD SET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(temp).append(".\n\t * @param v Value to Set.\n\t */");
                                        sb.append("\n\t").append("public void set").append(reformatted).append("(Integer v) {");
                                        sb.append("\n\t\t_").append(reformatted).append("=v;");
                                        sb.append("\n\t}");
                                    } else if (type.equalsIgnoreCase("string")) {
                                        String reformatted = formatted + "_" + formatFieldName(temp);
                                        sb.append("\n\n\t").append("//FIELD");
                                        sb.append("\n\n\t").append("private String _").append(reformatted);
                                        sb.append("=null;");

                                        //STANDARD GET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sb.append("\n\t").append("public String get").append(reformatted).append("(){");
                                        sb.append("\n\t\treturn _").append(reformatted).append(";");
                                        sb.append("\n\t}");

                                        //STANDARD SET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(temp).append(".\n\t * @param v Value to Set.\n\t */");
                                        sb.append("\n\t").append("public void set").append(reformatted).append("(String v){");
                                        sb.append("\n\t\t_").append(reformatted).append("=v;");
                                        sb.append("\n\t}");
                                    } else if (type.equalsIgnoreCase("date") || type.equalsIgnoreCase("dateTime") || type.equalsIgnoreCase("timestamp")) {
                                        String reformatted = formatted + "_" + formatFieldName(temp);
                                        sb.append("\n\n\t").append("//FIELD");
                                        sb.append("\n\n\t").append("private Date _").append(reformatted);
                                        sb.append("=null;");

                                        //STANDARD GET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sb.append("\n\t").append("public Date get").append(reformatted).append("(){");
                                        sb.append("\n\t\treturn _").append(reformatted).append(";");
                                        sb.append("\n\t}");

                                        //STANDARD SET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(temp).append(".\n\t * @param v Value to Set.\n\t */");
                                        sb.append("\n\t").append("public void set").append(reformatted).append("(Date v){");
                                        sb.append("\n\t\t_").append(reformatted).append("=v;");
                                        sb.append("\n\t}");
                                    } else {
                                        String reformatted = formatted + "_" + formatFieldName(temp);
                                        sb.append("\n\n\t").append("//FIELD");
                                        sb.append("\n\n\t").append("private Object _").append(reformatted);
                                        sb.append("=null;");

                                        //STANDARD GET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sb.append("\n\t").append("public Object get").append(reformatted).append("(){");
                                        sb.append("\n\t\treturn _").append(reformatted).append(";");
                                        sb.append("\n\t}");

                                        //STANDARD SET METHOD
                                        sb.append("\n\n");
                                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(temp).append(".\n\t * @param v Value to Set.\n\t */");
                                        sb.append("\n\t").append("public void set").append(reformatted).append("(Object v){");
                                        sb.append("\n\t\t_").append(reformatted).append("=v;");
                                        sb.append("\n\t}");
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                String type = f.getXMLType().getLocalType();
                if (type != null) {
                    String xmlPath   = f.getXMLPathString();
                    String formatted = formatFieldName(xmlPath);
                    if (formatted.equalsIgnoreCase("USER")) {
                        formatted = "userProperty";
                    }

                    xmlPaths.add(xmlPath);
                    if (f.getXMLType().getLocalType().equals("string")) {
                        if (f.getSize().equals("")) {
                            types.add(TYPE.data);
                        } else {
                            if (Integer.parseInt(f.getSize()) > 256) {
                                types.add(TYPE.LONG_DATA);
                            } else {
                                types.add(TYPE.data);
                            }
                        }
                    } else {
                        types.add(TYPE.data);
                    }
                    methods.add("set" + formatted);
                    gets.add("get" + formatted);
                    foreignElements.add("");

                    if (type.equalsIgnoreCase("boolean")) {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private Boolean _").append(formatted);
                        sb.append("=null;");

//                      STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public Boolean get").append(formatted).append("() {");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

//                        //STANDARD SET METHOD
//                        sb.append("\n\n");
//                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
//                        sb.append("\n\t").append("public void set").append(formatted).append("(Boolean v){");
//                        sb.append("\n\t\t_").append(formatted +"=v;");
//                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Object v){");
                        sb.append("\n\t\tif(v instanceof Boolean){");
                        sb.append("\n\t\t\t_").append(formatted).append("=(Boolean)v;");
                        sb.append("\n\t\t}else if(v instanceof String){");
                        sb.append("\n\t\t\t_").append(formatted).append("=formatBoolean((String)v);");
                        sb.append("\n\t\t}else if(v!=null){");
                        sb.append("\n\t\t\tthrow new IllegalArgumentException();");
                        sb.append("\n\t\t}");
                        sb.append("\n\t}");
                    } else if (type.equalsIgnoreCase("integer")) {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private Integer _").append(formatted);
                        sb.append("=null;");

//                      STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public Integer get").append(formatted).append("(){");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@JsonProperty");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Integer v) {");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");


                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@XmlTransient");
                        sb.append("\n\t").append("@JsonIgnore");
                        sb.append("\n\t").append("public void set").append(formatted).append("(String v)  {");
                        sb.append("\n\t\t_").append(formatted).append("=formatInteger(v);");
                        sb.append("\n\t}");
                    } else if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private Double _").append(formatted);
                        sb.append("=null;");

                        //STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public Double get").append(formatted).append("() {");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@JsonProperty");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Double v){");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@XmlTransient");
                        sb.append("\n\t").append("@JsonIgnore");
                        sb.append("\n\t").append("public void set").append(formatted).append("(String v)  {");
                        sb.append("\n\t\t_").append(formatted).append("=formatDouble(v);");
                        sb.append("\n\t}");
                    } else if (type.equalsIgnoreCase("string")) {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private String _").append(formatted);
                        sb.append("=null;");

                        //STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public String get").append(formatted).append("(){");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void set").append(formatted).append("(String v){");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");
                    } else if (StringUtils.equalsAnyIgnoreCase(type, "date", "dateTime")) {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private Date _").append(formatted);
                        sb.append("=null;");

                        //STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public Date get").append(formatted).append("(){");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@JsonProperty");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Date v){");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");

                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("@XmlTransient");
                        sb.append("\n\t").append("@JsonIgnore");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Object v){");
                        sb.append("\n\t\tthrow new IllegalArgumentException();");
                        sb.append("\n\t}");

                        if (type.equalsIgnoreCase("date")) {
                            sb.append("\n\n");
                            sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                            sb.append("\n\t").append("@XmlTransient");
                            sb.append("\n\t").append("@JsonIgnore");
                            sb.append("\n\t").append("public void set").append(formatted).append("(String v)  {");
                            sb.append("\n\t\t_").append(formatted).append("=formatDate(v);");
                            sb.append("\n\t}");
                        } else if (type.equalsIgnoreCase("dateTime")) {
                            sb.append("\n\n");
                            sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                            sb.append("\n\t").append("@XmlTransient");
                            sb.append("\n\t").append("@JsonIgnore");
                            sb.append("\n\t").append("public void set").append(formatted).append("(String v)  {");
                            sb.append("\n\t\t_").append(formatted).append("=formatDateTime(v);");
                            sb.append("\n\t}");
                        }
                    } else {
                        sb.append("\n\n\t").append("//FIELD");
                        sb.append("\n\n\t").append("private Object _").append(formatted);
                        sb.append("=null;");

                        //STANDARD GET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sb.append("\n\t").append("public Object get").append(formatted).append("(){");
                        sb.append("\n\t\treturn _").append(formatted).append(";");
                        sb.append("\n\t}");

                        //STANDARD SET METHOD
                        sb.append("\n\n");
                        sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sb.append("\n\t").append("public void set").append(formatted).append("(Object v){");
                        sb.append("\n\t\t_").append(formatted).append("=v;");
                        sb.append("\n\t}");
                    }
                }
            }
        }

        if (!element.containsStatedKey()) {
            GenericWrapperField f         = (GenericWrapperField) element.getDefaultKey();
            String              xmlPath   = f.getXMLPathString();
            String              formatted = formatFieldName(xmlPath);
            if (formatted.equalsIgnoreCase("USER")) {
                formatted = "userProperty";
            }
            sb.append("\n\n\t").append("//FIELD");
            sb.append("\n\n\t").append("private Integer _").append(formatted);
            sb.append("=null;");

            //STANDARD GET METHOD
            sb.append("\n\n");
            sb.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
            sb.append("\n\t").append("public Integer get").append(formatted).append("() {");
            sb.append("\n\t\treturn _").append(formatted).append(";");
            sb.append("\n\t}");

            //STANDARD SET METHOD
            sb.append("\n\n");
            sb.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
            sb.append("\n\t").append("public void set").append(formatted).append("(Integer v){");
            sb.append("\n\t\t_").append(formatted).append("=v;");
            sb.append("\n\t}");
        }

        //SET DATA FIELD
        sb.append("\n\n");
        sb.append("\t/**\n\t * Sets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        sb.append("\n\t").append("public void setDataField(String xmlPath,String v) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String method  = methods.get(i);

                if (type.equals(TYPE.data) || type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    } else {
                        sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    }
                    sb.append("\n\t\t\t").append(method).append("(v);");
                    count++;
                }
            }

            if (count > 0) {
                sb.append("\n\t\t").append("}");
                sb.append("\n\t\t").append("else{");
                sb.append("\n\t\t\t").append("super.setDataField(xmlPath,v);");

                sb.append("\n\t\t").append("}");
            } else {
                sb.append("\n\t\t\t").append("super.setDataField(xmlPath,v);");

            }
        } else {
            sb.append("\n\t\t").append("super.setDataField(xmlPath,v);");
        }
        sb.append("\n\t}");


        //SET REFERENCE FIELD
        sb.append("\n\n");
        sb.append("\t/**\n\t * Sets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        sb.append("\n\t").append("public void setReferenceField(String xmlPath,BaseElement v) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String method  = methods.get(i);

                if (!type.equals(TYPE.data) && !type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    } else {
                        sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    }
                    sb.append("\n\t\t\t").append(method).append("(v);");
                    count++;
                }
            }

            if (count > 0) {
                sb.append("\n\t\t").append("}");
                sb.append("\n\t\t").append("else{");
                sb.append("\n\t\t\t").append("super.setReferenceField(xmlPath,v);");

                sb.append("\n\t\t").append("}");
            } else {
                sb.append("\n\t\t\t").append("super.setReferenceField(xmlPath,v);");

            }
        } else {
            sb.append("\n\t\t").append("super.setReferenceField(xmlPath,v);");
        }
        sb.append("\n\t}");


        //SET DATA FIELD
        sb.append("\n\n");
        sb.append("\t/**\n\t * Gets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        sb.append("\n\t").append("public Object getDataFieldValue(String xmlPath) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String method  = gets.get(i);

                if (type.equals(TYPE.data) || type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    } else {
                        sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    }
                    sb.append("\n\t\t\treturn ").append(method).append("();");
                    count++;
                }
            }

            if (count > 0) {
                sb.append("\n\t\t").append("}");
                sb.append("\n\t\t").append("else{");
                sb.append("\n\t\t\t").append("return super.getDataFieldValue(xmlPath);");

                sb.append("\n\t\t").append("}");
            } else {
                sb.append("\n\t\t\t").append("return super.getDataFieldValue(xmlPath);");

            }
        } else {
            sb.append("\n\t\t").append("return super.getDataFieldValue(xmlPath);");
        }
        sb.append("\n\t}");

        //SET REFERENCE FIELD
        sb.append("\n\n");
        sb.append("\t/**\n\t * Gets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        sb.append("\n\t").append("public Object getReferenceField(String xmlPath) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String method  = gets.get(i);

                if (!type.equals(TYPE.data) && !type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    } else {
                        sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    }
                    sb.append("\n\t\t\treturn ").append(method).append("();");
                    count++;
                }
            }

            if (count > 0) {
                sb.append("\n\t\t").append("}");
                sb.append("\n\t\t").append("else{");
                sb.append("\n\t\t\t").append("return super.getReferenceField(xmlPath);");

                sb.append("\n\t\t").append("}");
            } else {
                sb.append("\n\t\t\t").append("return super.getReferenceField(xmlPath);");

            }
        } else {
            sb.append("\n\t\t").append("return super.getReferenceField(xmlPath);");
        }
        sb.append("\n\t}");


        //GET REFERENCE FIELD ELEMENT NAME
        sb.append("\n\n");
        sb.append("\t/**\n\t * Gets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        sb.append("\n\t").append("public String getReferenceFieldName(String xmlPath) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String foreign = foreignElements.get(i);

                if (!type.equals(TYPE.data) && !type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    } else {
                        sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                    }
                    sb.append("\n\t\t\treturn \"").append(foreign).append("\";");
                    count++;
                }
            }

            if (count > 0) {
                sb.append("\n\t\t").append("}");
                sb.append("\n\t\t").append("else{");
                sb.append("\n\t\t\t").append("return super.getReferenceFieldName(xmlPath);");

                sb.append("\n\t\t").append("}");
            } else {
                sb.append("\n\t\t\t").append("return super.getReferenceFieldName(xmlPath);");

            }
        } else {
            sb.append("\n\t\t").append("return super.getReferenceFieldName(xmlPath);");
        }
        sb.append("\n\t}");


        //GET FIELD TYPE
        sb.append("\n\n");
        sb.append("\t/**\n\t * Returns whether or not this is a reference field\n\t */");
        sb.append("\n\t").append("public String getFieldType(String xmlPath) throws BaseElement.UnknownFieldException{");

        if (xmlPaths.size() > 0) {
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);

                if (i == 0) {
                    sb.append("\n\t\t").append("if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                } else {
                    sb.append("\n\t\t").append("}else if (xmlPath.equals(\"").append(xmlPath).append("\")){");
                }
                if (type.equals(TYPE.data)) {
                    sb.append("\n\t\t\treturn BaseElement.field_data;");
                } else if (type.equals(TYPE.multi_reference)) {
                    sb.append("\n\t\t\treturn BaseElement.field_multi_reference;");
                } else if (type.equals(TYPE.single_reference)) {
                    sb.append("\n\t\t\treturn BaseElement.field_single_reference;");
                } else if (type.equals(TYPE.inline_repeater)) {
                    sb.append("\n\t\t\treturn BaseElement.field_inline_repeater;");
                } else if (type.equals(TYPE.LONG_DATA)) {
                    sb.append("\n\t\t\treturn BaseElement.field_LONG_DATA;");
                } else if (type.equals(TYPE.NO_CHILD)) {
                    sb.append("\n\t\t\treturn BaseElement.field_NO_CHILD;");
                }
            }

            sb.append("\n\t\t").append("}");
            sb.append("\n\t\t").append("else{");
            sb.append("\n\t\t\t").append("return super.getFieldType(xmlPath);");
            sb.append("\n\t\t").append("}");
        } else {
            sb.append("\n\t\t").append("return super.getFieldType(xmlPath);");
        }
        sb.append("\n\t}");


        //Get all fields
        sb.append("\n\n");
        sb.append("\t/**\n\t * Returns arraylist of all fields\n\t */");
        sb.append("\n\t").append("public ArrayList getAllFields() {");
        sb.append("\n\t\t").append("ArrayList all_fields=new ArrayList();");

        if (xmlPaths.size() > 0) {
            for (final String xmlPath : xmlPaths) {
                sb.append("\n\t\t").append("all_fields.add(\"").append(xmlPath).append("\");");
            }
            sb.append("\n\t\t").append("all_fields.addAll(super.getAllFields());");
            sb.append("\n\t\t").append("return all_fields;");
        } else {
            sb.append("\n\t\t").append("return super.getAllFields();");
        }
        sb.append("\n\t}");

        //WRITE XML
        sb.append("\n\n");
        sb.append("\n\t").append("public String toString(){");
        sb.append("\n\t\t").append("java.io.StringWriter sw = new java.io.StringWriter();");
        sb.append("\n\t\t").append("try{this.toXML(sw,true);}catch(java.io.IOException e){}");
        sb.append("\n\t\t").append("return sw.toString();");
        sb.append("\n\t").append("}");

        XMLWrapperElement xE = (XMLWrapperElement) XMLWrapperFactory.GetInstance().convertElement(element);

        //WRITE XML - WRITER
        sb.append("\n\n");
        sb.append("\n\t").append("public void toXML(java.io.Writer writer,boolean prettyPrint) throws java.io.IOException{");
        sb.append("\n\t\t").append("writer.write(\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\");");
        sb.append("\n\t\t").append("writer.write(\"\\n<").append(getRootName(xE)).append("\");");
        sb.append("\n\t\t").append("TreeMap map = new TreeMap();");
        sb.append("\n\t\t").append("map.putAll(getXMLAtts());");
        final Map<String, String> map = addXMLNSAttributes();
        for (final String key : map.keySet()) {
            sb.append("\n\t\t").append("map.put(\"").append(key).append("\",\"").append(map.get(key)).append("\");");
        }
        sb.append("\n\t\t").append("java.util.Iterator iter =map.keySet().iterator();");
        sb.append("\n\t\t").append("while(iter.hasNext()){");
        sb.append("\n\t\t\t").append("String key = (String)iter.next();");
        sb.append("\n\t\t\twriter.write(\" \" + key + \"=\\\"\" + map.get(key) + \"\\\"\");");
        sb.append("\n\t\t").append("}");

        sb.append("\n\t\t").append("int header = 0;");
        sb.append("\n\t\t").append("if (prettyPrint)header++;");
        sb.append("\n\t\t").append("writer.write(\">\");");

        sb.append("\n\t\t").append("addXMLBody(writer,header);");

        sb.append("\n\t\t").append("if (prettyPrint)header--;");
        sb.append("\n\t\t").append("writer.write(\"\\n</").append(getRootName(xE)).append(">\");");
        sb.append("\n\t").append("}");


        //WRITE XML Attributes - WRITER
        sb.append("\n\n");
        sb.append("\n\t").append("protected void addXMLAtts(java.io.Writer writer) throws java.io.IOException{");
        sb.append("\n\t\t").append("TreeMap map = this.getXMLAtts();");

        sb.append("\n\t\t").append("java.util.Iterator iter =map.keySet().iterator();");
        sb.append("\n\t\t").append("while(iter.hasNext()){");
        sb.append("\n\t\t\t").append("String key = (String)iter.next();");
        sb.append("\n\t\t\twriter.write(\" \" + key + \"=\\\"\" + map.get(key) + \"\\\"\");");
        sb.append("\n\t\t").append("}");

        sb.append("\n\t").append("}");


        //WRITE XML Attributes - WRITER
        sb.append("\n\n");
        sb.append("\n\t").append("protected TreeMap getXMLAtts() {");
        final Object[] attributesArray = xE.getAttributes().toArray();
        sb.append("\n\t\t").append("TreeMap map = super.getXMLAtts();");
        for (final Object o : attributesArray) {
            XMLWrapperField attField = (XMLWrapperField) o;
            if (!attField.isReference()) {
                String xmlPath   = attField.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                sb.append("\n\t\tif (_").append(formatted).append("!=null)");
                sb.append("\n\t\t\tmap.put(\"").append(attField.getName(false)).append("\",ValueParser(_").append(formatted).append(",\"").append(attField.getXMLType().getLocalType()).append("\"));");
                if (attField.isRequired()) {
                    sb.append("\n\t\telse map.put(\"").append(attField.getName(false)).append("\",\"\");//REQUIRED FIELD\n");
                } else {
                    sb.append("\n\t\t//NOT REQUIRED FIELD\n");
                }
            }
        }
        sb.append("\n\t\t").append("return map;");
        sb.append("\n\t").append("}");

        //WRITE XML Body tags - WRITER
        sb.append("\n\n");
        sb.append("\n\t").append("protected boolean addXMLBody(java.io.Writer writer, int header) throws java.io.IOException{");

        sb.append("\n\t\t").append("super.addXMLBody(writer,header);");
        int iterCounter = 0;
        for (final Object object : xE.getChildren()) {
            final XMLWrapperField xmlField = (XMLWrapperField) object;
            iterCounter = writeFieldToXMLCode(element, xmlField, iterCounter, sb);
        }

        if (element.isANoChildElement()) {
            sb.append("\n\t").append("return false;");
        } else {
            sb.append("\n\t").append("return true;");
        }

        sb.append("\n\t").append("}");


        //WRITE HAS XML CONTENT
        sb.append("\n\n");
        sb.append("\n\t").append("protected boolean hasXMLBodyContent(){");

        boolean returned = false;
        for (final Object object : xE.getChildren()) {
            if (returned) {
                break;
            }
            XMLWrapperField xmlField = (XMLWrapperField) object;
            if (xmlField.getExpose()) {
                String xmlPath   = xmlField.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                if (formatted.equalsIgnoreCase("USER")) {
                    formatted = "userProperty";
                }
                if (xmlField.isReference()) {
                    if (xmlField.isMultiple()) {
                        sb.append("\n\t\t").append("if(_").append(formatted).append(".size()>0) return true;");
                    } else {
                        if (!element.getExtensionFieldName().equalsIgnoreCase(xmlField.getName())) {
                            sb.append("\n\t\t").append("if (_").append(formatted).append("!=null){");
                            sb.append("\n\t\t\t").append("if (_").append(formatted).append(".hasXMLBodyContent()) return true;");
                            sb.append("\n\t\t").append("}");
                            if (!xmlField.getWrapped().getMinOccurs().equalsIgnoreCase("0")) {
                                sb.append("\n\t\t").append("return true;//REQUIRED ").append(xmlField.getXMLPathString());
                                returned = true;
                            } else {
                                sb.append("\n\t\t").append("//NOT REQUIRED\n");
                            }
                        }
                    }
                } else {
                    //NOT A REFERENCE
                    List<?> attAL = xmlField.getAttributes();
                    if (!attAL.isEmpty()) {
                        for (final Object attObject : attAL) {
                            XMLWrapperField attField     = (XMLWrapperField) attObject;
                            String          xmlPathATT   = attField.getXMLPathString();
                            String          formattedATT = formatFieldName(xmlPathATT);
                            sb.append("\n\t\tif (_").append(formattedATT).append("!=null)");
                            sb.append("\n\t\t\treturn true;");
                            if (attField.isRequired()) {
                                sb.append("\n\t\t").append("return true;//REQUIRED ").append(attField.getXMLPathString());
                                returned = true;
                                break;
                            }
                        }
                    }

                    if (!returned) {
                        if (xmlField.getChildren().size() > 0) {
                            for (Map.Entry<String, String> childVariable : this.getSubVariables(element, xmlField, true).entrySet()) {
                                if (childVariable.getValue().equals("MULTI")) {
                                    sb.append("\n\t\t\t").append("if(").append(childVariable.getKey()).append(".size()>0)return true;");
                                } else {
                                    sb.append("\n\t\t\t").append("if(").append(childVariable.getKey()).append("!=null) return true;");
                                }
                            }
                        } else {

                            if (xmlField.getXMLType() != null) {
                                sb.append("\n\t\t").append("if (_").append(formatted).append("!=null) return true;");
                                if (xmlField.isRequired()) {
                                    sb.append("\n\t\t").append("return true;//REQUIRED ").append(xmlField.getXMLPathString());
                                    returned = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!returned) {
            sb.append("\n\t\t").append("if(super.hasXMLBodyContent())return true;");

            sb.append("\n\t\t").append("return false;");
        }
        sb.append("\n\t").append("}");


        sb.append("\n");
        sb.append("}");

        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }

        File dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String dirStructure = packageName;
        while (dirStructure.contains(".")) {
            String folder = dirStructure.substring(0, dirStructure.indexOf("."));
            dirStructure = dirStructure.substring(dirStructure.indexOf(".") + 1);

            location = location + folder + File.separator;
            dir = new File(location);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        location = location + dirStructure + File.separator;
        dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (XFT.VERBOSE) {
            System.out.println("Generating File " + location + getFormattedBean(element) + ".java...");
        }
        try (final FileWriter writer = new FileWriter(location + getFormattedBean(element) + ".java")) {
            IOUtils.write(sb.toString(), writer);
        }
    }

    public Hashtable<String, String> getSubVariables(GenericWrapperElement e, XMLWrapperField xmlField, boolean isRoot) {
        Hashtable<String, String> subMethods = new Hashtable<>();

        if (xmlField.getExpose()) {
            String xmlPath   = xmlField.getXMLPathString();
            String formatted = formatFieldName(xmlPath);
            if (formatted.equalsIgnoreCase("USER")) {
                formatted = "userProperty";
            }
            if (xmlField.isReference()) {
                if (xmlField.isMultiple()) {
                    subMethods.put("_" + formatted, "MULTI");
                } else {
                    if (!e.getExtensionFieldName().equalsIgnoreCase(xmlField.getName())) {
                        subMethods.put("_" + formatted, "SINGLE");
                    }
                }
            } else {
                //NOT A REFERENCE
                List<?> attAL = xmlField.getAttributes();
                if (!attAL.isEmpty()) {
                    for (final Object attObject : attAL) {
                        XMLWrapperField attField     = (XMLWrapperField) attObject;
                        String          xmlPathATT   = attField.getXMLPathString();
                        String          formattedATT = formatFieldName(xmlPathATT);
                        if (isRoot) {
                            subMethods.put("_" + formattedATT, "ATT");
                        } else {
                            subMethods.put("_" + formattedATT, "SINGLE");
                        }
                    }
                }

                if (!xmlField.getChildren().isEmpty()) {
                    for (final Object object : xmlField.getChildren()) {
                        XMLWrapperField xwf = (XMLWrapperField) object;
                        if (xwf.getExpose()) {
                            subMethods.putAll(getSubVariables(e, xwf, false));
                        }
                    }
                } else {
                    if (xmlField.getXMLType() != null) {
                        subMethods.put("_" + formatted, "SINGLE");
                    }
                }
            }
        }

        return subMethods;
    }


    private int writeFieldToXMLCode(GenericWrapperElement e, XMLWrapperField xmlField, int iterCounter, StringBuffer sb) {
        if (xmlField.getExpose()) {
            String xmlPath   = xmlField.getXMLPathString();
            String formatted = formatFieldName(xmlPath);
            if (formatted.equalsIgnoreCase("USER")) {
                formatted = "userProperty";
            }
            if (xmlField.isReference()) {
                sb.append("\n\t\t").append("//REFERENCE FROM ").append(e.getXMLName()).append(" -> ").append(xmlField.getXMLPathString());
                try {
                    XMLWrapperElement foreign          = (XMLWrapperElement) xmlField.getReferenceElement();
                    String            foreignClassName = getFormattedBean(foreign.getGenericXFTElement());
                    if (xmlField.isMultiple()) {
                        if (xmlField.isInLineRepeaterElement()) {
                            sb.append("\n\t\t").append("//IN-LINE REPEATER");

                            sb.append("\n\t\t").append("java.util.Iterator iter").append(iterCounter).append("=_").append(formatted).append(".iterator();");
                            sb.append("\n\t\t").append("while(iter").append(iterCounter).append(".hasNext()){");
                            sb.append("\n\t\t\t").append(project).append(".bean.").append(foreignClassName).append(" child = (").append(project).append(".bean.").append(foreignClassName).append(")iter").append(iterCounter++).append(".next();");
                            sb.append("\n\t\t\t").append("child.addXMLBody(writer,header);");
                        } else {
                            sb.append("\n\t\t").append("java.util.Iterator iter").append(iterCounter).append("=_").append(formatted).append(".iterator();");
                            sb.append("\n\t\t").append("while(iter").append(iterCounter).append(".hasNext()){");
                            sb.append("\n\t\t\t").append(project).append(".bean.").append(foreignClassName).append(" child = (").append(project).append(".bean.").append(foreignClassName).append(")iter").append(iterCounter++).append(".next();");
                            sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                            sb.append("\n\t\t\t").append("child.addXMLAtts(writer);");
                            sb.append("\n\t\t\t").append("if(!child.getFullSchemaElementName().equals(\"").append(xmlField.getXMLType().getFullForeignType()).append("\")){");
                            sb.append("\n\t\t\t\t").append("writer.write(\" xsi:type=\\\"\" + child.getFullSchemaElementName() + \"\\\"\");");
                            sb.append("\n\t\t\t").append("}");
                            sb.append("\n\t\t\t").append("if (child.hasXMLBodyContent()){");
                            sb.append("\n\t\t\t\t").append("writer.write(\">\");");
                            sb.append("\n\t\t\t\t").append("boolean return").append(iterCounter).append(" =child.addXMLBody(writer,header);");
                            sb.append("\n\t\t\t\t").append("if(return").append(iterCounter).append("){");
                            sb.append("\n\t\t\t\t\t").append("writer.write(\"\\n\" + createHeader(--header) + \"</").append(xmlField.getName(true)).append(">\");");
                            sb.append("\n\t\t\t\t").append("}else{");
                            sb.append("\n\t\t\t\t\t").append("writer.write(\"</").append(xmlField.getName(true)).append(">\");");
                            sb.append("\n\t\t\t\t\t").append("header--;");
                            sb.append("\n\t\t\t\t").append("}");
                            sb.append("\n\t\t\t").append("}else {writer.write(\"/>\");header--;}");
                        }
                        sb.append("\n\t\t").append("}");
                    } else {
                        if (!e.getExtensionFieldName().equalsIgnoreCase(xmlField.getName())) {
                            sb.append("\n\t\t").append("//DATA-FIELD FROM ").append(e.getXMLName()).append(" -> ").append(xmlField.getXMLPathString());
                            sb.append("\n\t\t").append("if (_").append(formatted).append("!=null){");

                            boolean isNewElement = xmlField.isChildXMLNode();

                            if (!isNewElement) {
                                sb.append("\n\t\t").append("//NOT A NEW ELEMENT");
                                sb.append("\n\t\t\t").append("_").append(formatted).append(".addXMLBody(writer,header);");
                            } else {
                                sb.append("\n\t\t").append("//NEW ELEMENT");
                                sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                                sb.append("\n\t\t\t").append("_").append(formatted).append(".addXMLAtts(writer);");
                                sb.append("\n\t\t\t").append("if(!_").append(formatted).append(".getFullSchemaElementName().equals(\"").append(xmlField.getXMLType().getFullForeignType()).append("\")){");
                                sb.append("\n\t\t\t\t").append("writer.write(\" xsi:type=\\\"\" + _").append(formatted).append(".getFullSchemaElementName() + \"\\\"\");");
                                sb.append("\n\t\t\t").append("}");

                                sb.append("\n\t\t\t").append("if (_").append(formatted).append(".hasXMLBodyContent()){");
                                sb.append("\n\t\t\t\t").append("writer.write(\">\");");
                                sb.append("\n\t\t\t\t").append("boolean return").append(iterCounter).append(" =_").append(formatted).append(".addXMLBody(writer,header);");
                                sb.append("\n\t\t\t\t").append("if(return").append(iterCounter).append("){");
                                sb.append("\n\t\t\t\t\t").append("writer.write(\"\\n\" + createHeader(--header) + \"</").append(xmlField.getName(true)).append(">\");");
                                sb.append("\n\t\t\t\t").append("}else{");
                                sb.append("\n\t\t\t\t\t").append("writer.write(\"</").append(xmlField.getName(true)).append(">\");");
                                sb.append("\n\t\t\t\t\t").append("header--;");
                                sb.append("\n\t\t\t\t").append("}");
                                sb.append("\n\t\t\t").append("}else {writer.write(\"/>\");header--;}");
                            }
                            sb.append("\n\t\t").append("}");
                            if (!xmlField.getWrapped().getMinOccurs().equalsIgnoreCase("0")) {
                                sb.append("\n\t\t").append("else{");
                                sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header) + \"<").append(xmlField.getName(true)).append("/>\");//REQUIRED");
                                sb.append("\n\t\t").append("}");
                            } else {
                                sb.append("\n\t\t").append("//NOT REQUIRED\n");
                            }
                        }
                    }
                } catch (XFTInitException | ElementNotFoundException ignored) {
                }
            } else {
                //NOT A REFERENCE
                final List<?> attAL = xmlField.getAttributes();
                if (!attAL.isEmpty()) {
                    sb.append("\n\t\tTreeMap ").append(formatted).append("ATTMap = new TreeMap();");
                    sb.append("\n\t\tString ").append(formatted).append("ATT = new String();");
                    for (final Object object : attAL) {
                        XMLWrapperField attField     = (XMLWrapperField) object;
                        String          xmlPathATT   = attField.getXMLPathString();
                        String          formattedATT = formatFieldName(xmlPathATT);
                        sb.append("\n\t\tif (_").append(formattedATT).append("!=null)");
                        sb.append("\n\t\t\t").append(formatted).append("ATTMap.put(\"").append(attField.getName(false)).append("\",ValueParser(_").append(formattedATT).append(",\"").append(attField.getXMLType().getLocalType()).append("\"));");
                        if (attField.isRequired()) {
                            sb.append("\n\t\telse ").append(formatted).append("ATTMap.put(\"").append(attField.getName(false)).append("\",ValueParser(_").append(formattedATT).append(",\"").append(attField.getXMLType().getLocalType()).append("\"));//REQUIRED FIELD\n");
                        }
                    }
                    sb.append("\n\t\t").append("java.util.Iterator iter").append(iterCounter).append(" =").append(formatted).append("ATTMap.keySet().iterator();");
                    sb.append("\n\t\t").append("while(iter").append(iterCounter).append(".hasNext()){");
                    sb.append("\n\t\t\t").append("String key = (String)iter").append(iterCounter++).append(".next();");
                    sb.append("\n\t\t\t").append(formatted).append("ATT +=\" \" + key + \"=\\\"\" + ").append(formatted).append("ATTMap.get(key) + \"\\\"\";");
                    sb.append("\n\t\t").append("}");
                }

                if (xmlField.getChildren().size() > 0) {
                    sb.append("\n\t\t\t").append("int child").append(iterCounter).append("=0;");
                    sb.append("\n\t\t\t").append("int att").append(iterCounter).append("=0;");
                    for (Map.Entry<String, String> childVariable : this.getSubVariables(e, xmlField, true).entrySet()) {
                        if (childVariable.getValue().equals("MULTI")) {
                            sb.append("\n\t\t\t").append("child").append(iterCounter).append("+=").append(childVariable.getKey()).append(".size();");
                        } else if (childVariable.getValue().equals("ATT")) {
                            sb.append("\n\t\t\t").append("if(").append(childVariable.getKey()).append("!=null)");
                            sb.append("\n\t\t\t").append("att").append(iterCounter).append("++;");
                        } else {
                            sb.append("\n\t\t\t").append("if(").append(childVariable.getKey()).append("!=null)");
                            sb.append("\n\t\t\t").append("child").append(iterCounter).append("++;");
                        }
                    }

                    sb.append("\n\t\t\t").append("if(child").append(iterCounter).append(">0 || att").append(iterCounter).append(">0){");

                    int tempCounter = iterCounter;
                    iterCounter++;
                    StringBuffer childCode = new StringBuffer();
                    for (final Object object : xmlField.getChildren()) {
                        XMLWrapperField xwf = (XMLWrapperField) object;
                        if (xwf.getExpose()) {
                            iterCounter = writeFieldToXMLCode(e, xwf, iterCounter, childCode);
                        }
                    }

                    sb.append("\n\t\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                    if (attAL.size() > 0) {
                        sb.append("\n\t\t\t\t").append("writer.write(").append(formatted).append("ATT);");
                    }
                    sb.append("\n\t\t\t").append("if(child").append(tempCounter).append("==0){");
                    sb.append("\n\t\t\t\t").append("writer.write(\"/>\");");
                    sb.append("\n\t\t\t").append("}else{");

                    sb.append("\n\t\t\t\t").append("writer.write(\">\");");

                    sb.append(childCode);

                    sb.append("\n\t\t\t\t").append("writer.write(\"\\n\" + createHeader(--header) + \"</").append(xmlField.getName(true)).append(">\");");

                    sb.append("\n\t\t\t").append("}");
                    sb.append("\n\t\t\t").append("}\n");
                } else {

                    if (xmlField.getXMLType() == null) {
                        sb.append("\n\t\t").append("if(!").append(formatted).append("ATT.equals(\"\")){");
                        sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header) + \"<").append(xmlField.getName(true)).append("\");");
                        if (attAL.size() > 0) {
                            sb.append("\n\t\t\t").append("writer.write(").append(formatted).append("ATT);");
                        }
                        sb.append("\n\t\t\t").append("writer.write(\"/>\");");
                        sb.append("\n\t\t").append("}\n");
                    } else {
                        sb.append("\n\t\t").append("if (_").append(formatted).append("!=null){");
                        if (!e.isANoChildElement()) {
                            sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                            if (attAL.size() > 0) {
                                sb.append("\n\t\t\t").append("writer.write(").append(formatted).append("ATT);");
                            }
                            sb.append("\n\t\t\t").append("writer.write(\">\");");
                        }
                        sb.append("\n\t\t\t").append("writer.write(ValueParser(_").append(formatted).append(",\"").append(xmlField.getXMLType().getLocalType()).append("\"));");
                        if (!e.isANoChildElement()) {
                            sb.append("\n\t\t\t").append("writer.write(\"</").append(xmlField.getName(true)).append(">\");");
                        }
                        sb.append("\n\t\t\t").append("header--;");
                        sb.append("\n\t\t").append("}");
                        if (xmlField.isRequired()) {
                            sb.append("\n\t\t").append("else{");
                            sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                            if (attAL.size() > 0) {
                                sb.append("\n\t\t\t").append("writer.write(").append(formatted).append("ATT);");
                            }
                            sb.append("\n\t\t\t").append("writer.write(\"/>\");");
                            sb.append("\n\t\t\t").append("header--;");
                            sb.append("\n\t\t").append("}\n");
                        } else if (attAL.size() > 0) {
                            sb.append("\n\t\t").append("else if(!").append(formatted).append("ATT.equals(\"\")){");
                            sb.append("\n\t\t\t").append("writer.write(\"\\n\" + createHeader(header++) + \"<").append(xmlField.getName(true)).append("\");");
                            sb.append("\n\t\t\t").append("writer.write(").append(formatted).append("ATT);");
                            sb.append("\n\t\t\t").append("writer.write(\"/>\");");
                            sb.append("\n\t\t\t").append("header--;");
                            sb.append("\n\t\t").append("}\n");
                        }
                    }
                }
            }
        }

        return iterCounter;
    }

    public Map<String, String> addXMLNSAttributes() {
        //      ADD SCHEMA SPECIFICATION ATTRIBUTES
        final TreeMap<String, String> map      = new TreeMap<>();
        final Enumeration<?>          prefixes = XFTMetaManager.getPrefixEnum();
        while (prefixes.hasMoreElements()) {
            String prefix = (String) prefixes.nextElement();
            String uri    = XFTMetaManager.TranslatePrefixToURI(prefix);
            map.put("xmlns:" + prefix, uri);
        }
        map.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        return map;
    }

    public String getRootName(final XMLWrapperElement element) {
        final String alias = element.getProperName();
        return StringUtils.isNotBlank(alias) ? (alias.contains(":") ? alias : element.getSchemaTargetNamespacePrefix() + ":" + alias) : element.getFullXMLName();
    }

    public static String getFormattedBean(GenericWrapperElement e) {
        return XftStringUtils.FormatStringToClassName(e.getFormattedName()) + "Bean";
    }

    public static String getFormattedInterface(GenericWrapperElement e) {
        return XftStringUtils.FormatStringToClassName(e.getFormattedName()) + "I";
    }

    /**
     * @param s The file name.
     *
     * @return The formatted field name.
     */
    private String formatFieldName(String s) {
        return XftStringUtils.FormatStringToMethodSignature(s);
    }


    /**
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * @param project the project to set
     */
    public void setProject(String project) {
        this.project = project;
    }

    @SuppressWarnings("unused")
    public static void GenerateJavaFiles(final String name, final String javaLocation, final boolean skipXDAT, final String packageName, final String propsLocation) throws Exception {
        JavaBeanGenerator generator = new JavaBeanGenerator();
        generator.setProject(packageName);
        generator.generateJavaFiles(GenericUtils.convertToTypedList(XFTMetaManager.GetElementNames(), String.class), name, javaLocation, packageName, propsLocation);
    }

    public void generateJavaFiles(List<String> elementList, String name, String javaLocation, String rootPackage, String propsLocation) throws Exception {
        Hashtable<String, String> elements = new Hashtable<>();
        for (String s : elementList) {
            GenericWrapperElement e = GenericWrapperElement.GetElement(s);
            if (e.getAddin().equalsIgnoreCase("")) {
                elements.put(e.getSchemaTargetNamespaceURI() + ":" + e.getLocalXMLName(), rootPackage + ".bean." + getFormattedBean(e));
                if (!e.getProperName().equals(e.getFullXMLName())) {
                    elements.put(e.getSchemaTargetNamespaceURI() + ":" + e.getProperName(), rootPackage + ".bean." + getFormattedBean(e));
                }
                this.generateJavaBeanFile(e, javaLocation);
                this.generateJavaInterface(e, javaLocation);
            }
        }

        StringBuilder sb          = new StringBuilder();
        String        packageName = rootPackage + ".bean";
        for (Map.Entry<String, String> entry : elements.entrySet()) {
            sb.append(entry.getKey().replace(":", "\\:")).append("=").append(entry.getValue()).append("\n");
        }

        outputToFile(propsLocation, sb.toString(), name + "-bean-definition.properties", packageName);
    }

    public static void outputToFile(String location, String contents, String fileName, String packageName) {
        File dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String dirStructure = packageName;
        while (dirStructure.contains(".")) {
            String folder = dirStructure.substring(0, dirStructure.indexOf("."));
            dirStructure = dirStructure.substring(dirStructure.indexOf(".") + 1);

            location = location + folder + File.separator;
            dir = new File(location);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        location = location + dirStructure + File.separator;
        dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (XFT.VERBOSE) {
            System.out.println("Generating File " + location + fileName);
        }
        try (final FileWriter writer = new FileWriter(location + fileName)) {
            IOUtils.write(contents, writer);
        } catch (IOException e) {
            log.error("An error occurred trying to write the file {} to the folder {}", fileName, location, e);
        }
    }

    /**
     * @param element  The element
     * @param location The location where the Java interface file should be written.
     *
     * @throws Exception When something goes wrong.
     */
    public void generateJavaInterface(GenericWrapperElement element, String location) throws Exception {
        //INTERFACE
        final String interfaceExtensionName = element.isExtension() ? getFormattedInterface(GenericWrapperElement.GetElement(element.getExtensionType())) : null;
        final String interfaceName          = getFormattedInterface(element);
        final String beanName               = getFormattedBean(element);

        final StringBuilder sbI = new StringBuilder();

        sbI.append("/*\n * GENERATED FILE\n * Created on ").append(Calendar.getInstance().getTime()).append("\n * JavaBeanGenerator.generateJavaInterface(GenericWrapperElement, String)\n */");
        sbI.append("\npackage " + INTERFACE_PACKAGE + ";");

        //IMPORTS
        sbI.append("\n\nimport com.fasterxml.jackson.databind.annotation.JsonDeserialize;");
        sbI.append("\nimport org.nrg.xdat.bean.").append(beanName).append(";");
        sbI.append("\n\nimport java.util.List;");
        sbI.append("\n\n/**\n * @author XDAT\n *\n */");

        sbI.append("\n@JsonDeserialize(as = ").append(beanName).append(".class)");
        sbI.append("\npublic interface ").append(interfaceName);
        if (interfaceExtensionName == null) {
            sbI.append(" {");
        } else {
            sbI.append(" extends ").append(interfaceExtensionName).append(" {");
        }

        sbI.append("\n\n");
        sbI.append("\t").append("public String getXSIType();");
        sbI.append("\n\n");
        sbI.append("\t").append("public void toXML(java.io.Writer writer) throws java.lang.Exception;");


        for (final Object object : element.getAllFields(true, true)) {
            GenericWrapperField f = (GenericWrapperField) object;
            if (f.isReference()) {
                String xmlPath   = f.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                if (formatted.equalsIgnoreCase("USER")) {
                    formatted = "userProperty";
                }
                SchemaElementI foreign = f.getReferenceElement();

                if (foreign.getGenericXFTElement().getAddin().equals("")) {
                    String foreignClassName = getFormattedInterface(foreign.getGenericXFTElement());

                    if (f.isMultiple()) {
                        sbI.append("\n");
                        sbI.append("\n\t/**");
                        sbI.append("\n\t * ").append(xmlPath);
                        sbI.append("\n\t * @return Returns an List of org.nrg.xdat.model.").append(foreignClassName).append("\n\t */");
                        sbI.append("\n\t").append("public <A extends ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append("> List<A> get").append(formatted).append("();");

                        sbI.append("\n");
                        sbI.append("\n\t/**");
                        sbI.append("\n\t * ").append(xmlPath);
                        sbI.append("\n\t * @return Returns an List of org.nrg.xdat.model.").append(foreignClassName).append("\n\t */");
                        sbI.append("\n\t").append("public <A extends ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append("> void add").append(formatted).append("(A item) throws Exception;");
                    } else {

                        if (!f.getName().equalsIgnoreCase(element.getExtensionFieldName())) {
                            sbI.append("\n");
                            sbI.append("\n\t/**");
                            sbI.append("\n\t * ").append(xmlPath);
                            sbI.append("\n\t * @return ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append("\n\t */");
                            sbI.append("\n\t").append("public ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append(" get").append(formatted).append("();");

                            sbI.append("\n");
                            sbI.append("\n\t/**");
                            sbI.append("\n\t * ").append(xmlPath);
                            sbI.append("\n\t * @return ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append("\n\t */");
                            sbI.append("\n\t").append("public <A extends ").append(INTERFACE_PACKAGE + ".").append(foreignClassName).append("> void set").append(formatted).append("(A item) throws Exception;");

                            for (final XFTRelationSpecification spec : ((XFTSuperiorReference) f.getXFTReference()).getKeyRelations()) {
                                String temp = spec.getLocalCol();
                                String type = spec.getSchemaType().getLocalType();
                                if (type != null) {
                                    if (type.equalsIgnoreCase("integer")) {
                                        String reformatted = formatted + "FK";

                                        //STANDARD GET METHOD
                                        sbI.append("\n\n");
                                        sbI.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sbI.append("\n\t").append("public Integer get").append(reformatted).append("();");

                                    } else if (type.equalsIgnoreCase("string")) {
                                        String reformatted = formatted + "_" + formatFieldName(temp);

                                        //STANDARD GET METHOD
                                        sbI.append("\n\n");
                                        sbI.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sbI.append("\n\t").append("public String get").append(reformatted).append("();");

                                    } else {
                                        String reformatted = formatted + "_" + formatFieldName(temp);

                                        //STANDARD GET METHOD
                                        sbI.append("\n\n");
                                        sbI.append("\t/**\n\t * @return Returns the ").append(element.getXSIType()).append("/").append(temp).append(".\n\t */");
                                        sbI.append("\n\t").append("public Object get").append(reformatted).append("();");

                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                String type = f.getXMLType().getLocalType();
                if (type != null) {
                    String xmlPath   = f.getXMLPathString();
                    String formatted = formatFieldName(xmlPath);
                    if (formatted.equalsIgnoreCase("USER")) {
                        formatted = "userProperty";
                    }

                    if (type.equalsIgnoreCase("boolean")) {
//                      STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public Boolean get").append(formatted).append("();");


                        //STANDARD SET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(Object v);");
                    } else if (type.equalsIgnoreCase("integer")) {
                        //STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public Integer get").append(formatted).append("();");

                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(element.getXSIType()).append("/").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(Integer v) ;");

                    } else if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
                        //STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public Double get").append(formatted).append("();");

                        //STANDARD SET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(Double v);");

                    } else if (type.equalsIgnoreCase("string")) {
                        //STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public String get").append(formatted).append("();");

                        //STANDARD SET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(String v);");

                    } else if (type.equalsIgnoreCase("date") || type.equalsIgnoreCase("dateTime")) {
                        //STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public Object get").append(formatted).append("();");

                        //STANDARD SET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(Object v);");

                    } else {
                        //STANDARD GET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
                        sbI.append("\n\t").append("public Object get").append(formatted).append("();");

                        //STANDARD SET METHOD
                        sbI.append("\n\n");
                        sbI.append("\t/**\n\t * Sets the value for ").append(xmlPath).append(".\n\t * @param v Value to Set.\n\t */");
                        sbI.append("\n\t").append("public void set").append(formatted).append("(Object v);");

                    }
                }
            }
        }

        if (!element.containsStatedKey()) {
            GenericWrapperField f         = (GenericWrapperField) element.getDefaultKey();
            String              xmlPath   = f.getXMLPathString();
            String              formatted = formatFieldName(xmlPath);
            if (formatted.equalsIgnoreCase("USER")) {
                formatted = "userProperty";
            }

            //STANDARD GET METHOD
            sbI.append("\n\n");
            sbI.append("\t/**\n\t * @return Returns the ").append(xmlPath).append(".\n\t */");
            sbI.append("\n\t").append("public Integer get").append(formatted).append("();");
        }
        sbI.append("\n");
        sbI.append("}");

        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }

        File dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String dirStructure = INTERFACE_PACKAGE;
        while (dirStructure.contains(".")) {
            String folder = dirStructure.substring(0, dirStructure.indexOf("."));
            dirStructure = dirStructure.substring(dirStructure.indexOf(".") + 1);

            location = location + folder + File.separator;
            dir = new File(location);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        location = location + dirStructure + File.separator;
        dir = new File(location);
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (XFT.VERBOSE) {
            System.out.println("Generating File " + location + getFormattedInterface(element) + ".java...");
        }
        try (final FileWriter writer = new FileWriter(location + getFormattedInterface(element) + ".java")) {
            IOUtils.write(sbI.toString(), writer);
        }
    }
}

