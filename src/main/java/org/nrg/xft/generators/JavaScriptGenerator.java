/*
 * core: org.nrg.xft.generators.JavaScriptGenerator
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.*/

package org.nrg.xft.generators;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.meta.XFTMetaManager;
import org.nrg.xft.references.XFTReferenceI;
import org.nrg.xft.references.XFTRelationSpecification;
import org.nrg.xft.references.XFTSuperiorReference;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperElement;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperFactory;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperField;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.schema.design.XFTFieldWrapper;
import org.nrg.xft.utils.XftStringUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaScriptGenerator {
    private enum TYPE {data, single_reference, multi_reference, inline_repeater, LONG_DATA, NO_CHILD}

    /**
     * @param element  The element from which to generate JavaScript
     * @param location The location of the folder where the JavaScript file should be written.
     *
     * @throws Exception When an error occurs.
     */
    public void generateJSFile(final GenericWrapperElement element, final String location) throws Exception {
        final StringBuilder builder = new StringBuilder();

        builder.append("/*\n * GENERATED FILE\n * Created on ").append(Calendar.getInstance().getTime()).append("\n * JavaScriptGenerator.generateJSFile(GenericWrapperElement, String)\n */");
        //IMPORTS
        builder.append("\n\n/**\n * @author XDAT\n *\n */");

        //CLASS
        String sqlName = element.getFormattedName();
        builder.append("\n");
        builder.append("\n").append("function ").append(sqlName).append("(){");
        builder.append("\n").append("this.xsiType=\"").append(element.getXSIType()).append("\";");

        builder.append("\n\n");
        builder.append("\t").append("this.getSchemaElementName=function(){");
        builder.append("\n\t\t").append("return \"").append(element.getLocalXMLName()).append("\";");
        builder.append("\n\t}");
        builder.append("\n\n");
        builder.append("\t").append("this.getFullSchemaElementName=function(){");
        builder.append("\n\t\t").append("return \"").append(element.getFullXMLName()).append("\";");
        builder.append("\n\t}");

        XMLWrapperElement xE = (XMLWrapperElement) XMLWrapperFactory.GetInstance().convertElement(element);

//      CLASS
        if (element.isExtension()) {
            GenericWrapperElement ext = GenericWrapperElement.GetElement(element.getExtensionType());
            if (!ext.getXSIType().equals(element.getXSIType())) {
                builder.append("\n").append("this.extension=dynamicJSLoad('").append(ext.getFormattedName()).append("','generated/").append(ext.getFormattedName()).append(".js');");
            }
        }

        ArrayList<String> xmlPaths        = new ArrayList<>();
        ArrayList<TYPE>   types           = new ArrayList<>();
        ArrayList<String> methods         = new ArrayList<>();
        ArrayList<String> foreignElements = new ArrayList<>();

        for (final GenericWrapperField f : GenericUtils.convertToTypedList(element.getAllFields(true, true), GenericWrapperField.class)) {
            if (f.isReference()) {
                String         xmlPath   = f.getXMLPathString();
                String         formatted = formatFieldName(xmlPath);
                SchemaElementI foreign   = f.getReferenceElement();

                if (foreign.getGenericXFTElement().getAddin().equals("")) {

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
                        foreignElements.add(foreign.getGenericXFTElement().getSchemaTargetNamespaceURI() + ":" + foreign.getGenericXFTElement().getLocalXMLName());

                        builder.append("\n\tthis.").append(formatted).append(" =new Array();");
                        builder.append("\n");
                        builder.append("\n\tfunction ").append("get").append(formatted).append("() {");
                        builder.append("\n\t\treturn this.").append(formatted).append(";");
                        builder.append("\n\t}");
                        builder.append("\n\tthis.").append("get").append(formatted).append("=").append("get").append(formatted).append(";");

                        builder.append("\n\n");
                        builder.append("\n\t").append("function add").append(formatted).append("(v){");
                        builder.append("\n\t\tthis.").append(formatted).append(".push(v);");
                        builder.append("\n\t}");
                        builder.append("\n\tthis.").append("add").append(formatted).append("=").append("add").append(formatted).append(";");

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
                            foreignElements.add(foreign.getGenericXFTElement().getSchemaTargetNamespaceURI() + ":" + foreign.getGenericXFTElement().getLocalXMLName());
                            builder.append("\n\tthis." + formatted + " =null;");
                            builder.append("\n\t").append("function get").append(formatted).append("() {");
                            builder.append("\n\t\treturn this." + formatted + ";");
                            builder.append("\n\t}");
                            builder.append("\n\tthis.get").append(formatted).append("=get").append(formatted).append(";");

                            builder.append("\n\n");
                            builder.append("\n\t").append("function set").append(formatted).append("(v){");
                            builder.append("\n\t\tthis.").append(formatted + " =v;");
                            builder.append("\n\t}");
                            builder.append("\n\tthis.set").append(formatted).append("=set").append(formatted).append(";");


                            XFTSuperiorReference ref  = (XFTSuperiorReference) f.getXFTReference();
                            Iterator             iter = ref.getKeyRelations().iterator();
                            while (iter.hasNext()) {
                                XFTRelationSpecification spec = (XFTRelationSpecification) iter.next();
                                String                   temp = spec.getLocalCol();

                                String reformatted = formatted + "_" + formatFieldName(temp);
                                builder.append("\n\n\t").append("this.").append(reformatted);
                                builder.append("=null;");

                                //STANDARD GET METHOD
                                builder.append("\n\n");
                                builder.append("\n\t").append("function get").append(reformatted).append("(){");
                                builder.append("\n\t\treturn this." + reformatted + ";");
                                builder.append("\n\t}");
                                builder.append("\n\tthis.get").append(reformatted).append("=get").append(reformatted).append(";");

                                //STANDARD SET METHOD
                                builder.append("\n\n");
                                builder.append("\n\t").append("function set").append(reformatted).append("(v){");
                                builder.append("\n\t\tthis.").append(reformatted + "=v;");
                                builder.append("\n\t}");
                                builder.append("\n\tthis.set").append(reformatted).append("=set").append(reformatted).append(";");

                            }
                        }
                    }
                }
            } else {
                String type = f.getXMLType().getLocalType();
                if (type != null) {
                    String xmlPath   = f.getXMLPathString();
                    String formatted = formatFieldName(xmlPath);

                    xmlPaths.add(xmlPath);
                    if (f.getXMLType().getLocalType().equals("string")) {
                        if (f.getSize().equals("")) {
                            types.add(TYPE.data);
                        } else {
                            int size = Integer.valueOf(f.getSize()).intValue();
                            if (size > 256) {
                                types.add(TYPE.LONG_DATA);
                            } else {
                                types.add(TYPE.data);
                            }
                        }
                    } else {
                        types.add(TYPE.data);
                    }
                    methods.add("set" + formatted);
                    foreignElements.add("");

                    builder.append("\n\n\t").append("this.").append(formatted);
                    builder.append("=null;");

//                      STANDARD GET METHOD
                    builder.append("\n\n");
                    builder.append("\n\t").append("function get").append(formatted).append("() {");
                    builder.append("\n\t\treturn this.").append(formatted).append(";");
                    builder.append("\n\t}");
                    builder.append("\n\tthis.get").append(formatted).append("=get").append(formatted).append(";");

                    //STANDARD SET METHOD
                    builder.append("\n\n");
                    builder.append("\n\t").append("function set").append(formatted).append("(v){");
                    builder.append("\n\t\tthis.").append(formatted).append("=v;");
                    builder.append("\n\t}");
                    builder.append("\n\tthis.set").append(formatted).append("=set").append(formatted).append(";");

                    if (type.equals("boolean")) {
                        builder.append("\n\n");
                        builder.append("\n\t").append("this.is").append(formatted).append("=function(defaultValue) {");
                        builder.append("\n\t\tif(this.").append(formatted).append("==null)return defaultValue;");
                        builder.append("\n\t\tif(this.").append(formatted).append("==\"1\" || this.").append(formatted).append("==true)return true;");
                        builder.append("\n\t\treturn false;");
                        builder.append("\n\t}");
                    }
                }
            }
        }

        for (final XFTFieldWrapper xwf : element.getAddIns()) {
            final GenericWrapperField field     = (GenericWrapperField) xwf;
            final String              xmlPath   = field.getXMLPathString();
            final String              formatted = formatFieldName(xmlPath);

            if (field.isReference()) {
                if (!field.isMultiple()) {
                    XFTReferenceI ref = field.getXFTReference();
                    if (!ref.isManyToMany()) {
                        XFTSuperiorReference sup = (XFTSuperiorReference) ref;
                        for (final XFTRelationSpecification spec : sup.getKeyRelations()) {
                            if (StringUtils.isNotBlank(spec.getLocalCol())) {
                                builder.append("\n\n\t").append("this.").append(spec.getLocalCol());
                                builder.append("_fk=null;");

                                //STANDARD GET METHOD
                                builder.append("\n\n");
                                builder.append("\n\t").append("this.get").append(spec.getLocalCol()).append("=function() {");
                                builder.append("\n\t\treturn this.").append(spec.getLocalCol()).append("_fk;");
                                builder.append("\n\t}");

                                //STANDARD SET METHOD
                                builder.append("\n\n");
                                builder.append("\n\t").append("this.set").append(spec.getLocalCol()).append("=function(v){");
                                builder.append("\n\t\tthis.").append(spec.getLocalCol()).append("_fk=v;");
                                builder.append("\n\t}");
                            }
                        }
                    }
                }
            } else {
                builder.append("\n\n\t").append("this.").append(formatted);
                builder.append("=null;");

                //STANDARD GET METHOD
                builder.append("\n\n");
                builder.append("\n\t").append("function get").append(formatted).append("() {");
                builder.append("\n\t\treturn this.").append(formatted).append(";");
                builder.append("\n\t}");
                builder.append("\n\tthis.get").append(formatted).append("=get").append(formatted).append(";");

                //STANDARD SET METHOD
                builder.append("\n\n");
                builder.append("\n\t").append("function set").append(formatted).append("(v){");
                builder.append("\n\t\tthis.").append(formatted).append("=v;");
                builder.append("\n\t}");
                builder.append("\n\tthis.set").append(formatted).append("=set").append(formatted).append(";");
            }
        }

        //GET PROPERTY
        builder.append("\n\n");
        builder.append("\n\t").append("this.getProperty=function(xmlPath){");

        builder.append("\n\t\t\t").append("if(xmlPath.startsWith(this.getFullSchemaElementName())){");
        builder.append("\n\t\t\t\t").append("xmlPath=xmlPath.substring(this.getFullSchemaElementName().length + 1);");
        builder.append("\n\t\t\t").append("}");

        for (final GenericWrapperField field : GenericUtils.convertToTypedList(element.getAllFields(true, true), GenericWrapperField.class)) {
            final String xmlPath   = field.getXMLPathString();
            final String formatted = formatFieldName(xmlPath);

            builder.append("\n\t\t\t").append("if(xmlPath==\"").append(xmlPath).append("\"){");
            builder.append("\n\t\t\t\t").append("return this.").append(formatted).append(" ;");
            builder.append("\n\t\t\t").append("} else ");

            if (field.isReference()) {
                SchemaElementI foreign = field.getReferenceElement();
                if (foreign.getGenericXFTElement().getAddin().equals("")) {
                    builder.append("\n\t\t\t").append("if(xmlPath.startsWith(\"" + xmlPath + "\")){");
                    builder.append("\n\t\t\t\t").append("xmlPath=xmlPath.substring(").append(xmlPath.length()).append(");");
                    builder.append("\n\t\t\t\t").append("if(xmlPath==\"\")return this.").append(formatted).append(" ;");
                    builder.append("\n\t\t\t\t").append("if(xmlPath.startsWith(\"[\")){");

                    builder.append("\n\t\t\t\t\t").append("if (xmlPath.indexOf(\"/\")>-1){");
                    builder.append("\n\t\t\t\t\t\t").append("var optionString=xmlPath.substring(0,xmlPath.indexOf(\"/\"));");
                    builder.append("\n\t\t\t\t\t\t").append("xmlPath=xmlPath.substring(xmlPath.indexOf(\"/\")+1);");
                    builder.append("\n\t\t\t\t\t").append("}else{");
                    builder.append("\n\t\t\t\t\t\t").append("var optionString=xmlPath;");
                    builder.append("\n\t\t\t\t\t\t").append("xmlPath=\"\";");
                    builder.append("\n\t\t\t\t\t").append("}");

                    builder.append("\n\t\t\t\t\t");
                    builder.append("\n\t\t\t\t\t").append("var options = loadOptions(optionString);//omUtils.js");

                    builder.append("\n\t\t\t\t").append("}else{xmlPath=xmlPath.substring(1);}");
                    if (field.isMultiple()) {

                        builder.append("\n\t\t\t\t").append("var index=0;");
                        builder.append("\n\t\t\t\t").append("if(options){");
                        builder.append("\n\t\t\t\t\t").append("if(options.index)index=options.index;");
                        builder.append("\n\t\t\t\t").append("}");

                        builder.append("\n\n\t\t\t").append("var whereArray;");
                        builder.append("\n\t\t\t\t").append("if (options.where){");
                        builder.append("\n\n\t\t\t\t").append("whereArray=new Array();");
                        builder.append("\n\n\t\t\t\t").append("for(var whereCount=0;whereCount<this.").append(formatted).append(".length;whereCount++){");
                        builder.append("\n\n\t\t\t\t\t").append("var tempValue=this.").append(formatted).append("[whereCount].getProperty(options.where.field);");
                        builder.append("\n\n\t\t\t\t\t").append("if(tempValue!=null)if(tempValue.toString()==options.where.value.toString()){");
                        builder.append("\n\n\t\t\t\t\t\t").append("whereArray.push(this.").append(formatted).append("[whereCount]);");
                        builder.append("\n\n\t\t\t\t\t").append("}");
                        builder.append("\n\n\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\n\t\t\t\t").append("whereArray=this.").append(formatted).append(";");
                        builder.append("\n\t\t\t\t").append("}");


                        builder.append("\n\n\t\t\t").append("var typeArray;");
                        builder.append("\n\t\t\t\t").append("if (options.xsiType){");
                        builder.append("\n\n\t\t\t\t").append("typeArray=new Array();");
                        builder.append("\n\n\t\t\t\t").append("for(var typeCount=0;typeCount<whereArray.length;typeCount++){");
                        builder.append("\n\n\t\t\t\t\t").append("if(whereArray[typeCount].getFullSchemaElementName()==options.xsiType){");
                        builder.append("\n\n\t\t\t\t\t\t").append("typeArray.push(whereArray[typeCount]);");
                        builder.append("\n\n\t\t\t\t\t").append("}");
                        builder.append("\n\n\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\n\t\t\t\t").append("typeArray=whereArray;");
                        builder.append("\n\t\t\t\t").append("}");

                        builder.append("\n\t\t\t\t").append("if (typeArray.length>index){");
                        builder.append("\n\t\t\t\t\t").append("return typeArray[index].getProperty(xmlPath);");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\t\t\t\t\t").append("return null;");
                        builder.append("\n\t\t\t\t").append("}");

                    } else {
                        builder.append("\n\t\t\t\t").append("if(this.").append(formatted).append("!=undefined)return this.").append(formatted).append(".getProperty(xmlPath);");
                        builder.append("\n\t\t\t\t").append("else return null;");
                    }
                    builder.append("\n\t\t\t").append("} else ");
                }
            }
        }

        for (final XFTFieldWrapper xwf : element.getAddIns()) {
            final GenericWrapperField gwf       = (GenericWrapperField) xwf;
            final String              xmlPath   = gwf.getXMLPathString();
            final String              formatted = formatFieldName(xmlPath);

            if (gwf.isReference()) {
                if (!gwf.isMultiple()) {
                    XFTReferenceI ref = gwf.getXFTReference();
                    if (!ref.isManyToMany()) {
                        for (XFTRelationSpecification spec : ((XFTSuperiorReference) ref).getKeyRelations()) {
                            if (StringUtils.isNotBlank(spec.getLocalCol())) {
                                builder.append("\n\t\t\t").append("if(xmlPath==\"").append(spec.getLocalCol()).append("\"){");
                                builder.append("\n\t\t\t\t").append("return this.").append(spec.getLocalCol()).append("_fk ;");
                                builder.append("\n\t\t\t").append("} else ");
                            }
                        }
                    }
                }
            } else {
                builder.append("\n\t\t\t").append("if(xmlPath==\"").append(xmlPath).append("\"){");
                builder.append("\n\t\t\t\t").append("return this.").append(formatted).append(" ;");
                builder.append("\n\t\t\t").append("} else ");
            }
        }

        builder.append("\n\t\t\t").append("{");
        if (element.isExtension()) {
            builder.append("\n\t\t\t\t").append("return this.extension.getProperty(xmlPath);");
        } else {
            builder.append("\n\t\t\t\t").append("return null;");
        }
        builder.append("\n\t\t\t}\n\t}");

        //SET PROPERTY
        builder.append("\n\n");
        builder.append("\n\t").append("this.setProperty=function(xmlPath,value){");
        builder.append("\n\t\t\t").append("if(xmlPath.startsWith(this.getFullSchemaElementName())){");
        builder.append("\n\t\t\t\t").append("xmlPath=xmlPath.substring(this.getFullSchemaElementName().length + 1);");
        builder.append("\n\t\t\t").append("}");

        for (final GenericWrapperField field : GenericUtils.convertToTypedList(element.getAllFields(true, true), GenericWrapperField.class)) {
            final String xmlPath   = field.getXMLPathString();
            final String formatted = formatFieldName(xmlPath);

            builder.append("\n\t\t\t").append("if(xmlPath==\"").append(xmlPath).append("\"){");
            builder.append("\n\t\t\t\t").append("this.").append(formatted).append("=value;");
            builder.append("\n\t\t\t").append("} else ");

            if (field.isReference()) {
                SchemaElementI foreign = field.getReferenceElement();
                if (foreign.getGenericXFTElement().getAddin().equals("")) {
                    builder.append("\n\t\t\t").append("if(xmlPath.startsWith(\"" + xmlPath + "\")){");
                    builder.append("\n\t\t\t\t").append("xmlPath=xmlPath.substring(").append(xmlPath.length()).append(");");
                    builder.append("\n\t\t\t\t").append("if(xmlPath==\"\")return this.").append(formatted).append(" ;");
                    builder.append("\n\t\t\t\t").append("if(xmlPath.startsWith(\"[\")){");

                    builder.append("\n\t\t\t\t\t").append("if (xmlPath.indexOf(\"/\")>-1){");
                    builder.append("\n\t\t\t\t\t\t").append("var optionString=xmlPath.substring(0,xmlPath.indexOf(\"/\"));");
                    builder.append("\n\t\t\t\t\t\t").append("xmlPath=xmlPath.substring(xmlPath.indexOf(\"/\")+1);");
                    builder.append("\n\t\t\t\t\t").append("}else{");
                    builder.append("\n\t\t\t\t\t\t").append("var optionString=xmlPath;");
                    builder.append("\n\t\t\t\t\t\t").append("xmlPath=\"\";");
                    builder.append("\n\t\t\t\t\t").append("}");

                    builder.append("\n\t\t\t\t\t");
                    builder.append("\n\t\t\t\t\t").append("var options = loadOptions(optionString);//omUtils.js");

                    builder.append("\n\t\t\t\t").append("}else{xmlPath=xmlPath.substring(1);}");
                    if (field.isMultiple()) {

                        builder.append("\n\t\t\t\t").append("var index=0;");
                        builder.append("\n\t\t\t\t").append("if(options){");
                        builder.append("\n\t\t\t\t\t").append("if(options.index)index=options.index;");
                        builder.append("\n\t\t\t\t").append("}");

                        builder.append("\n\n\t\t\t").append("var whereArray;");
                        builder.append("\n\t\t\t\t").append("if (options && options.where){");
                        builder.append("\n\n\t\t\t\t").append("whereArray=new Array();");
                        builder.append("\n\n\t\t\t\t").append("for(var whereCount=0;whereCount<this.").append(formatted).append(".length;whereCount++){");
                        builder.append("\n\n\t\t\t\t\t").append("var tempValue=this.").append(formatted).append("[whereCount].getProperty(options.where.field);");
                        builder.append("\n\n\t\t\t\t\t").append("if(tempValue!=null)if(tempValue.toString()==options.where.value.toString()){");
                        builder.append("\n\n\t\t\t\t\t\t").append("whereArray.push(this.").append(formatted).append("[whereCount]);");
                        builder.append("\n\n\t\t\t\t\t").append("}");
                        builder.append("\n\n\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\n\t\t\t\t").append("whereArray=this.").append(formatted).append(";");
                        builder.append("\n\t\t\t\t").append("}");


                        builder.append("\n\n\t\t\t").append("var typeArray;");
                        builder.append("\n\t\t\t\t").append("if (options && options.xsiType){");
                        builder.append("\n\n\t\t\t\t").append("typeArray=new Array();");
                        builder.append("\n\n\t\t\t\t").append("for(var typeCount=0;typeCount<whereArray.length;typeCount++){");
                        builder.append("\n\n\t\t\t\t\t").append("if(whereArray[typeCount].getFullSchemaElementName()==options.xsiType){");
                        builder.append("\n\n\t\t\t\t\t\t").append("typeArray.push(whereArray[typeCount]);");
                        builder.append("\n\n\t\t\t\t\t").append("}");
                        builder.append("\n\n\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\n\t\t\t\t").append("typeArray=whereArray;");
                        builder.append("\n\t\t\t\t").append("}");


                        builder.append("\n\t\t\t\t").append("if (typeArray.length>index){");
                        builder.append("\n\t\t\t\t\t").append("typeArray[index].setProperty(xmlPath,value);");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\t\t\t\t\t").append("var newChild;");
                        builder.append("\n\t\t\t\t\t").append("if(options && options.xsiType){");
                        builder.append("\n\t\t\t\t\t\t").append("newChild= instanciateObject(options.xsiType);//omUtils.js");
                        builder.append("\n\t\t\t\t\t").append("}else{");
                        builder.append("\n\t\t\t\t\t\t").append("newChild= instanciateObject(\"").append(field.getReferenceElementName().getFullForeignType()).append("\");//omUtils.js");
                        builder.append("\n\t\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t\t").append("this.add").append(formatted).append("(newChild);");
                        builder.append("\n\t\t\t\t\t").append("if(options && options.where)newChild.setProperty(options.where.field,options.where.value);");
                        builder.append("\n\t\t\t\t\t").append("newChild.setProperty(xmlPath,value);");
                        builder.append("\n\t\t\t\t").append("}");

                    } else {
                        builder.append("\n\t\t\t\t").append("if(this.").append(formatted).append("!=undefined){");
                        builder.append("\n\t\t\t\t\t").append("this.").append(formatted).append(".setProperty(xmlPath,value);");
                        builder.append("\n\t\t\t\t").append("}else{");
                        builder.append("\n\t\t\t\t\t\t").append("if(options && options.xsiType){");
                        builder.append("\n\t\t\t\t\t\t\t").append("this.").append(formatted).append("= instanciateObject(options.xsiType);//omUtils.js");
                        builder.append("\n\t\t\t\t\t\t").append("}else{");
                        builder.append("\n\t\t\t\t\t\t\t").append("this.").append(formatted).append("= instanciateObject(\"").append(field.getReferenceElementName().getFullForeignType()).append("\");//omUtils.js");
                        builder.append("\n\t\t\t\t\t\t").append("}");
                        builder.append("\n\t\t\t\t\t\t").append("if(options && options.where)this.").append(formatted).append(".setProperty(options.where.field,options.where.value);");
                        builder.append("\n\t\t\t\t\t\t").append("this.").append(formatted).append(".setProperty(xmlPath,value);");
                        builder.append("\n\t\t\t\t").append("}");

                    }
                    builder.append("\n\t\t\t").append("} else ");
                }

            }
        }

        for (final XFTFieldWrapper xwf : element.getAddIns()) {
            final GenericWrapperField gwf       = (GenericWrapperField) xwf;
            String                    xmlPath   = gwf.getXMLPathString();
            String                    formatted = formatFieldName(xmlPath);

            if (gwf.isReference()) {
                if (!gwf.isMultiple()) {
                    XFTReferenceI ref = gwf.getXFTReference();
                    if (!ref.isManyToMany()) {
                        for (final XFTRelationSpecification spec : ((XFTSuperiorReference) ref).getKeyRelations()) {
                            if (StringUtils.isNotBlank(spec.getLocalCol())) {
                                builder.append("\n\t\t\t").append("if(xmlPath==\"").append(spec.getLocalCol()).append("\"){");
                                builder.append("\n\t\t\t\t").append("this.").append(spec.getLocalCol()).append("_fk=value;");
                                builder.append("\n\t\t\t").append("} else ");
                            }
                        }
                    }
                }
            } else {
                builder.append("\n\t\t\t").append("if(xmlPath==\"").append(xmlPath).append("\"){");
                builder.append("\n\t\t\t\t").append("this.").append(formatted).append("=value;");
                builder.append("\n\t\t\t").append("} else ");
            }
        }
        builder.append("\n\t\t\t").append("{");
        if (element.isExtension()) {
            builder.append("\n\t\t\t\t").append("return this.extension.setProperty(xmlPath,value);");
        } else {
            builder.append("\n\t\t\t\t").append("return null;");
        }
        builder.append("\n\t\t\t").append("}");
        builder.append("\n\t").append("}");

        //SET REFERENCE FIELD
        builder.append("\n\n");
        builder.append("\t/**\n\t * Sets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        builder.append("\n\t").append("this.setReferenceField=function(xmlPath,v) {");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String method  = methods.get(i);

                if (!type.equals(TYPE.data) && !type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        builder.append("\n\t\t").append("if (xmlPath==\"").append(xmlPath).append("\"){");
                    } else {
                        builder.append("\n\t\t").append("}else if (xmlPath==\"").append(xmlPath).append("\"){");
                    }
                    builder.append("\n\t\t\tthis.").append(method).append("(v);");
                    count++;
                }
            }

            if (count > 0) {
                builder.append("\n\t\t").append("}");
                builder.append("\n\t\t").append("else{");
                if (element.isExtension()) {
                    builder.append("\n\t\t\t").append("this.extension.setReferenceField(xmlPath,v);");
                }

                builder.append("\n\t\t").append("}");
            } else {
                if (element.isExtension()) {
                    builder.append("\n\t\t\t").append("this.extension.setReferenceField(xmlPath,v);");
                }
            }
        } else {
            if (element.isExtension()) {
                builder.append("\n\t\t").append("this.extension.setReferenceField(xmlPath,v);");
            }
        }
        builder.append("\n\t}");


        //GET REFERENCE FIELD ELEMENT NAME
        builder.append("\n\n");
        builder.append("\t/**\n\t * Gets the value for a field via the XMLPATH.\n\t * @param v Value to Set.\n\t */");
        builder.append("\n\t").append("this.getReferenceFieldName=function(xmlPath) {");

        if (xmlPaths.size() > 0) {
            int count = 0;
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);
                String foreign = foreignElements.get(i);

                if (!type.equals(TYPE.data) && !type.equals(TYPE.LONG_DATA)) {
                    if (count == 0) {
                        builder.append("\n\t\t").append("if (xmlPath==\"").append(xmlPath).append("\"){");
                    } else {
                        builder.append("\n\t\t").append("}else if (xmlPath==\"").append(xmlPath).append("\"){");
                    }
                    builder.append("\n\t\t\treturn \"").append(foreign).append("\";");
                    count++;
                }
            }

            if (count > 0) {
                builder.append("\n\t\t").append("}");
                builder.append("\n\t\t").append("else{");
                if (element.isExtension()) {
                    builder.append("\n\t\t\t").append("return this.extension.getReferenceFieldName(xmlPath);");
                }
                builder.append("\n\t\t").append("}");
            } else if (element.isExtension()) {
                builder.append("\n\t\t\t").append("return this.extension.getReferenceFieldName(xmlPath);");
            }
        } else if (element.isExtension()) {
            builder.append("\n\t\t").append("return this.extension.getReferenceFieldName(xmlPath);");
        }
        builder.append("\n\t}");


        //GET FIELD TYPE
        builder.append("\n\n");
        builder.append("\t/**\n\t * Returns whether or not this is a reference field\n\t */");
        builder.append("\n\t").append("this.getFieldType=function(xmlPath){");

        if (!xmlPaths.isEmpty()) {
            for (int i = 0; i < xmlPaths.size(); i++) {
                String xmlPath = xmlPaths.get(i);
                TYPE   type    = types.get(i);

                if (i == 0) {
                    builder.append("\n\t\t").append("if (xmlPath==\"").append(xmlPath).append("\"){");
                } else {
                    builder.append("\n\t\t").append("}else if (xmlPath==\"").append(xmlPath).append("\"){");
                }
                if (type.equals(TYPE.data)) {
                    builder.append("\n\t\t\treturn \"field_data\";");
                } else if (type.equals(TYPE.multi_reference)) {
                    builder.append("\n\t\t\treturn \"field_multi_reference\";");
                } else if (type.equals(TYPE.single_reference)) {
                    builder.append("\n\t\t\treturn \"field_single_reference\";");
                } else if (type.equals(TYPE.inline_repeater)) {
                    builder.append("\n\t\t\treturn \"field_inline_repeater\";");
                } else if (type.equals(TYPE.LONG_DATA)) {
                    builder.append("\n\t\t\treturn \"field_LONG_DATA\";");
                } else if (type.equals(TYPE.NO_CHILD)) {
                    builder.append("\n\t\t\treturn \"field_NO_CHILD\";");
                }
            }

            builder.append("\n\t\t").append("}");
            builder.append("\n\t\t").append("else{");
            if (element.isExtension()) {
                builder.append("\n\t\t\t").append("return this.extension.getFieldType(xmlPath);");
            }
            builder.append("\n\t\t").append("}");
        } else {
            if (element.isExtension()) {
                builder.append("\n\t\t").append("return this.extension.getFieldType(xmlPath);");
            }
        }
        builder.append("\n\t}");


        //WRITE XML - WRITER
        builder.append("\n\n");
        builder.append("\n\t").append("this.toXML=function(xmlTxt,preventComments){");
        builder.append("\n\t\t").append("xmlTxt+=\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\";");
        builder.append("\n\t\t").append("xmlTxt+=\"\\n<").append(getRootName(xE)).append("\";");
        builder.append("\n\t\t").append("xmlTxt+=this.getXMLAtts();");

        Map      map  = this.addXMLNSAttributes();
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            builder.append("\n\t\t").append("xmlTxt+=\" " + key + "=\\\"" + map.get(key) + "\\\"\";");
        }

        builder.append("\n\t\t").append("xmlTxt+=\">\";");

        builder.append("\n\t\t").append("xmlTxt+=this.getXMLBody(preventComments)");

        builder.append("\n\t\t").append("xmlTxt+=\"\\n</").append(getRootName(xE)).append(">\";");
        builder.append("\n\t\t").append("return xmlTxt;");

        builder.append("\n\t").append("}");


        builder.append("\n\n");
        builder.append("\n\t").append("this.getXMLComments=function(preventComments){");
        builder.append("\n\t\t").append("var str =\"\";");
        builder.append("\n\t\t").append("if((preventComments==undefined || !preventComments) && this.hasXMLComments()){");

        if (!element.getAddIns().isEmpty()) {
            builder.append("\n\t\t").append("str += \"<!--hidden_fields[\";");
            builder.append("\n\t\t").append("var hiddenCount = 0;");
            for (XFTFieldWrapper xwf : element.getAddIns()) {
                GenericWrapperField gwf       = (GenericWrapperField) xwf;
                String              xmlPath   = gwf.getXMLPathString();
                String              formatted = formatFieldName(xmlPath);

                if (gwf.isReference()) {
                    if (!gwf.isMultiple()) {
                        XFTReferenceI ref = gwf.getXFTReference();
                        if (!ref.isManyToMany()) {
                            XFTSuperiorReference sup   = (XFTSuperiorReference) ref;
                            Iterator             specs = sup.getKeyRelations().iterator();
                            while (specs.hasNext()) {
                                XFTRelationSpecification spec = (XFTRelationSpecification) specs.next();
                                if (spec.getLocalCol() != null && spec.getLocalCol() != "") {
                                    builder.append("\n\t\t\t").append("if(this." + spec.getLocalCol() + "_fk!=null){");
                                    builder.append("\n\t\t\t\t").append("if(hiddenCount++>0)str+=\",\";");
                                    builder.append("\n\t\t\t\t").append("str+=\"" + spec.getLocalCol() + "=\\\"\" + this." + spec.getLocalCol() + "_fk + \"\\\"\";");
                                    builder.append("\n\t\t\t").append("}");
                                }
                            }
                        }
                    }
                } else {
                    builder.append("\n\t\t\t").append("if(this." + formatted + "!=null){");
                    builder.append("\n\t\t\t\t").append("if(hiddenCount++>0)str+=\",\";");
                    builder.append("\n\t\t\t\t").append("str+=\"" + xmlPath + "=\\\"\" + this." + formatted + " + \"\\\"\";");
                    builder.append("\n\t\t\t").append("}");
                }
            }
            builder.append("\n\t\t").append("str +=\"]-->\";");
        }
        builder.append("\n\t\t").append("}");
        builder.append("\n\t\t").append("return str;");
        builder.append("\n\t").append("}");


        //WRITE XML Attributes - WRITER
        builder.append("\n\n");
        builder.append("\n\t").append("this.getXMLAtts=function(){");

        Object[] attributesArray = xE.getAttributes().toArray();
        if (element.isExtension()) {
            builder.append("\n\t\t").append("var attTxt = this.extension.getXMLAtts();");
        } else {
            builder.append("\n\t\t").append("var attTxt = \"\";");
        }
        for (final Object object : attributesArray) {
            XMLWrapperField attField = (XMLWrapperField) object;
            if (!attField.isReference()) {
                String xmlPath   = attField.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                builder.append("\n\t\tif (this.").append(formatted).append("!=null)");
                builder.append("\n\t\t\tattTxt+=\" ").append(attField.getName(false)).append("=\\\"\" +this.").append(formatted).append(" +\"\\\"\";");
                if (attField.isRequired()) {
                    builder.append("\n\t\telse attTxt+=\" ").append(attField.getName(false)).append("=\\\"\\\"\";//REQUIRED FIELD\n");
                } else {
                    builder.append("\n\t\t//NOT REQUIRED FIELD\n");
                }
            }
        }
        builder.append("\n\t\t").append("return attTxt;");
        builder.append("\n\t").append("}");

        //WRITE XML Body tags - WRITER
        builder.append("\n\n");
        builder.append("\n\t").append("this.getXMLBody=function(preventComments){");
        builder.append("\n\t\t").append("var xmlTxt=this.getXMLComments(preventComments);");
        if (element.isExtension()) {
            builder.append("\n\t\t").append("xmlTxt+=this.extension.getXMLBody(preventComments);");
        }

        final AtomicInteger iterCounter = new AtomicInteger();
        for (final XMLWrapperField xmlField : GenericUtils.convertToTypedList(xE.getChildren(), XMLWrapperField.class)) {
            iterCounter.set(writeFieldToXMLCode(element, xmlField, iterCounter, builder));
        }

        builder.append("\n\t\t").append("return xmlTxt;");
        builder.append("\n\t").append("}");

        //WRITE HAS XML CONTENT
        builder.append("\n\n");
        builder.append("\n\t").append("this.hasXMLComments=function(){");
        if (!element.getAddIns().isEmpty()) {
            for (XFTFieldWrapper xwf : element.getAddIns()) {
                GenericWrapperField gwf       = (GenericWrapperField) xwf;
                String              xmlPath   = gwf.getXMLPathString();
                String              formatted = formatFieldName(xmlPath);

                if (gwf.isReference()) {
                    if (!gwf.isMultiple()) {
                        XFTReferenceI ref = gwf.getXFTReference();
                        if (!ref.isManyToMany()) {
                            for (final XFTRelationSpecification spec : ((XFTSuperiorReference) ref).getKeyRelations()) {
                                if (StringUtils.isNotBlank(spec.getLocalCol())) {
                                    builder.append("\n\t\t\t").append("if (this." + spec.getLocalCol() + "_fk!=null) return true;");
                                }
                            }
                        }
                    }
                } else {
                    builder.append("\n\t\t\t").append("if (this." + formatted + "!=null) return true;");
                }
            }
            builder.append("\n\t\t\t").append("return false;");
        }
        builder.append("\n\t").append("}");

        //WRITE HAS XML CONTENT
        builder.append("\n\n");
        builder.append("\n\t").append("this.hasXMLBodyContent=function(){");

        boolean returned = false;
        for (final XMLWrapperField xmlField : GenericUtils.convertToTypedList(xE.getChildren(), XMLWrapperField.class)) {
            if (xmlField.getExpose()) {
                String xmlPath   = xmlField.getXMLPathString();
                String formatted = formatFieldName(xmlPath);
                if (xmlField.isReference()) {
                    try {
                        XMLWrapperElement foreign = (XMLWrapperElement) xmlField.getReferenceElement();
                        if (xmlField.isMultiple()) {
                            builder.append("\n\t\t").append("if(this." + formatted + ".length>0) return true;");
                        } else {
                            if (!StringUtils.equalsIgnoreCase(element.getExtensionFieldName(), xmlField.getName())) {
                                builder.append("\n\t\t").append("if (this." + formatted + "!=null){");
                                builder.append("\n\t\t\t").append("if (this." + formatted + ".hasXMLBodyContent()) return true;");
                                builder.append("\n\t\t").append("}");
                                if (!xmlField.getWrapped().getMinOccurs().equalsIgnoreCase("0")) {
                                    builder.append("\n\t\t").append("return true;//REQUIRED " + xmlField.getXMLPathString());
                                    returned = true;
                                } else {
                                    builder.append("\n\t\t").append("//NOT REQUIRED\n");
                                }
                            }
                        }
                    } catch (XFTInitException e1) {
                    } catch (ElementNotFoundException e1) {
                    }
                } else {
                    //NOT A REFERENCE

                    ArrayList attAL = xmlField.getAttributes();
                    if (attAL.size() > 0) {
                        Iterator attributes = xmlField.getAttributes().iterator();
                        while (attributes.hasNext()) {
                            XMLWrapperField attField     = (XMLWrapperField) attributes.next();
                            String          fieldID      = attField.getId();
                            String          xmlPathATT   = attField.getXMLPathString();
                            String          formattedATT = formatFieldName(xmlPathATT);
                            builder.append("\n\t\tif (this.").append(formattedATT).append("!=null)");
                            builder.append("\n\t\t\treturn true;");
                            if (attField.isRequired()) {
                                builder.append("\n\t\t").append("return true;//REQUIRED " + attField.getXMLPathString());
                                returned = true;
                                break;
                            }
                        }
                    }

                    if (!returned) {
                        if (xmlField.getChildren().size() > 0) {
                            for (Map.Entry<String, String> childVariable : getSubVariables(element, xmlField, true).entrySet()) {
                                if (childVariable.getValue().equals("MULTI")) {
                                    builder.append("\n\t\t\t").append("if(this." + childVariable.getKey() + ".length>0)return true;");
                                } else {
                                    builder.append("\n\t\t\t").append("if(this." + childVariable.getKey() + "!=null) return true;");
                                }
                            }
                        } else {

                            if (xmlField.getXMLType() == null) {
                            } else {
                                builder.append("\n\t\t").append("if (this." + formatted + "!=null) return true;");
                                if (xmlField.isRequired()) {
                                    builder.append("\n\t\t").append("return true;//REQUIRED " + xmlField.getXMLPathString());
                                    returned = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!returned) {
            builder.append("\n\t\t").append("if(this.hasXMLComments())return true;");
            if (element.isExtension()) {
                builder.append("\n\t\t").append("if(this.extension.hasXMLBodyContent())return true;");
            }
            builder.append("\n\t\t").append("return false;");
        }
        builder.append("\n\t").append("}");
        builder.append("\n");
        builder.append("}");

        final File finalLocation = Paths.get(location, "generated").toFile();
        finalLocation.mkdirs();

        if (XFT.VERBOSE) {
            System.out.println("Generating File " + finalLocation.getAbsolutePath() + File.separator + sqlName + ".js...");
        }
        try (final Reader reader = new StringReader(builder.toString());
             final Writer writer = new FileWriter(new File(finalLocation, sqlName + ".js"))) {
            IOUtils.copy(reader, writer);
        }
    }

    public Hashtable<String, String> getSubVariables(GenericWrapperElement e, XMLWrapperField xmlField, boolean isRoot) {
        final Hashtable<String, String> subMethods = new Hashtable<>();

        if (xmlField.getExpose()) {
            String xmlPath   = xmlField.getXMLPathString();
            String formatted = formatFieldName(xmlPath);
            if (xmlField.isReference()) {
                if (xmlField.isMultiple()) {
                    subMethods.put(formatted, "MULTI");
                } else {
                    if (!e.getExtensionFieldName().equalsIgnoreCase(xmlField.getName())) {
                        subMethods.put(formatted, "SINGLE");
                    }
                }
            } else {
                //NOT A REFERENCE
                for (final XMLWrapperField attField : GenericUtils.convertToTypedList(xmlField.getAttributes(), XMLWrapperField.class)) {
                    String xmlPathATT   = attField.getXMLPathString();
                    String formattedATT = formatFieldName(xmlPathATT);
                    if (isRoot) {
                        subMethods.put(formattedATT, "ATT");
                    } else {
                        subMethods.put(formattedATT, "SINGLE");
                    }
                }

                final List<XMLWrapperField> children = GenericUtils.convertToTypedList(xmlField.getChildren(), XMLWrapperField.class);

                if (!children.isEmpty()) {
                    for (final XMLWrapperField xwf : children) {
                        if (xwf.getExpose()) {
                            subMethods.putAll(getSubVariables(e, xwf, false));
                        }
                    }
                } else if (xmlField.getXMLType() != null) {
                    subMethods.put(formatted, "SINGLE");
                }
            }
        }

        return subMethods;
    }


    public int writeFieldToXMLCode(final GenericWrapperElement element, final XMLWrapperField xmlField, final AtomicInteger iterCounter, final StringBuilder sb) {
        if (xmlField.getExpose()) {
            String xmlPath   = xmlField.getXMLPathString();
            String formatted = formatFieldName(xmlPath);
            if (xmlField.isReference()) {
                if (xmlField.isMultiple()) {
                    String formattedCount = formatted + "COUNT";
                    if (xmlField.isInLineRepeaterElement()) {

                        sb.append("\n\t\t").append("for(var " + formattedCount + "=0;" + formattedCount + "<this." + formatted + ".length;" + formattedCount + "++){");
                        sb.append("\n\t\t\t").append("xmlTxt+=this." + formatted + "[" + formattedCount + "].getXMLBody(preventComments);");
                        sb.append("\n\t\t").append("}");
                    } else {
                        sb.append("\n\t\t").append("for(var " + formattedCount + "=0;" + formattedCount + "<this." + formatted + ".length;" + formattedCount + "++){");
                        sb.append("\n\t\t\t").append("xmlTxt +=\"\\n<" + xmlField.getName(true) + "\";");
                        sb.append("\n\t\t\t").append("xmlTxt +=this." + formatted + "[" + formattedCount + "].getXMLAtts();");
                        sb.append("\n\t\t\t").append("if(this." + formatted + "[" + formattedCount + "].xsiType!=\"" + xmlField.getXMLType().getFullForeignType() + "\"){");
                        sb.append("\n\t\t\t\t").append("xmlTxt+=\" xsi:type=\\\"\" + this." + formatted + "[" + formattedCount + "].xsiType + \"\\\"\";");
                        sb.append("\n\t\t\t").append("}");
                        sb.append("\n\t\t\t").append("if (this." + formatted + "[" + formattedCount + "].hasXMLBodyContent()){");
                        sb.append("\n\t\t\t\t").append("xmlTxt+=\">\";");
                        sb.append("\n\t\t\t\t").append("xmlTxt+=this." + formatted + "[" + formattedCount + "].getXMLBody(preventComments);");

                        sb.append("\n\t\t\t\t\t").append("xmlTxt+=\"</" + xmlField.getName(true) + ">\";");

                        sb.append("\n\t\t\t").append("}else {xmlTxt+=\"/>\";}");


                        sb.append("\n\t\t").append("}");
                    }
                } else {
                    if (!element.getExtensionFieldName().equalsIgnoreCase(xmlField.getName())) {
                        sb.append("\n\t\t").append("if (this." + formatted + "!=null){");

                        boolean isNewElement = true;
                        if (!xmlField.isChildXMLNode()) {
                            isNewElement = false;
                        }

                        if (!isNewElement) {
                            sb.append("\n\t\t\t").append("xmlTxt+=this." + formatted + ".getXMLBody(writer,header);");
                        } else {
                            sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<" + xmlField.getName(true) + "\";");
                            sb.append("\n\t\t\t").append("xmlTxt+=this." + formatted + ".getXMLAtts();");
                            sb.append("\n\t\t\t").append("if(this." + formatted + ".xsiType!=\"" + xmlField.getXMLType().getFullForeignType() + "\"){");
                            sb.append("\n\t\t\t\t").append("xmlTxt+=\" xsi:type=\\\"\" + this." + formatted + ".xsiType + \"\\\"\";");
                            sb.append("\n\t\t\t").append("}");

                            sb.append("\n\t\t\t").append("if (this." + formatted + ".hasXMLBodyContent()){");
                            sb.append("\n\t\t\t\t").append("xmlTxt+=\">\";");
                            sb.append("\n\t\t\t\t").append("xmlTxt+=this." + formatted + ".getXMLBody(preventComments);");
                            sb.append("\n\t\t\t\t").append("xmlTxt+=\"</" + xmlField.getName(true) + ">\";");
                            sb.append("\n\t\t\t").append("}else {xmlTxt+=\"/>\";}");
                        }
                        sb.append("\n\t\t").append("}");
                        if (!xmlField.getWrapped().getMinOccurs().equalsIgnoreCase("0")) {
                            sb.append("\n\t\t").append("else{");
                            sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<" + xmlField.getName(true) + "/>\";//REQUIRED");
                            sb.append("\n\t\t").append("}");
                        } else {
                            sb.append("\n\t\t").append("//NOT REQUIRED\n");
                        }
                    }
                }
            } else {
                //NOT A REFERENCE
                final List<XMLWrapperField> attAL = GenericUtils.convertToTypedList(xmlField.getAttributes(), XMLWrapperField.class);
                if (!attAL.isEmpty()) {
                    sb.append("\n\t\tvar ").append(formatted).append("ATT = \"\"");
                    for (final XMLWrapperField attField : attAL) {
                        String xmlPathATT   = attField.getXMLPathString();
                        String formattedATT = formatFieldName(xmlPathATT);
                        sb.append("\n\t\tif (this.").append(formattedATT).append("!=null)");

                        if (attField.getXMLType().getLocalType().equals("string")) {
                            sb.append("\n\t\t\t").append(formatted).append("ATT+=\" ").append(attField.getName(false)).append("=\\\"\" + this.").append(formattedATT).append(".replace(/>/g,\"&gt;\").replace(/</g,\"&lt;\") + \"\\\"\";");
                        } else {
                            sb.append("\n\t\t\t").append(formatted).append("ATT+=\" ").append(attField.getName(false)).append("=\\\"\" + this.").append(formattedATT).append(" + \"\\\"\";");
                        }

                        if (attField.isRequired()) {
                            sb.append("\n\t\telse ").append(formatted).append("ATT+=\" ").append(attField.getName(false)).append("=\"+ this.").append(formattedATT).append(" +\"\\\"\";//REQUIRED FIELD\n");

                        }
                    }
                }

                if (xmlField.getChildren().size() > 0) {
                    sb.append("\n\t\t\t").append("var child").append(iterCounter).append("=0;");
                    sb.append("\n\t\t\t").append("var att").append(iterCounter).append("=0;");
                    for (Map.Entry<String, String> childVariable : this.getSubVariables(element, xmlField, true).entrySet()) {
                        if (childVariable.getValue().equals("MULTI")) {
                            sb.append("\n\t\t\t").append("child").append(iterCounter).append("+=this.").append(childVariable.getKey()).append(".length;");
                        } else if (childVariable.getValue().equals("ATT")) {
                            sb.append("\n\t\t\t").append("if(this.").append(childVariable.getKey()).append("!=null)");
                            sb.append("\n\t\t\t").append("att").append(iterCounter).append("++;");
                        } else {
                            sb.append("\n\t\t\t").append("if(this.").append(childVariable.getKey()).append("!=null)");
                            sb.append("\n\t\t\t").append("child").append(iterCounter).append("++;");
                        }
                    }

                    sb.append("\n\t\t\t").append("if(child").append(iterCounter).append(">0 || att").append(iterCounter).append(">0){");

                    int tempCounter = iterCounter.get();
                    iterCounter.incrementAndGet();
                    StringBuilder childCode = new StringBuilder();
                    for (final XMLWrapperField xwf : GenericUtils.convertToTypedList(xmlField.getChildren(), XMLWrapperField.class)) {
                        if (xwf.getExpose()) {
                            iterCounter.set(writeFieldToXMLCode(element, xwf, iterCounter, childCode));
                        }
                    }

                    sb.append("\n\t\t\t\t").append("xmlTxt+=\"\\n<").append(xmlField.getName(true)).append("\";");
                    if (attAL.size() > 0) {
                        sb.append("\n\t\t\t\t").append("xmlTxt+=").append(formatted).append("ATT;");
                    }
                    sb.append("\n\t\t\t").append("if(child").append(tempCounter).append("==0){");
                    sb.append("\n\t\t\t\t").append("xmlTxt+=\"/>\";");
                    sb.append("\n\t\t\t").append("}else{");

                    sb.append("\n\t\t\t\t").append("xmlTxt+=\">\";");

                    sb.append(childCode);

                    sb.append("\n\t\t\t\t").append("xmlTxt+=\"\\n</").append(xmlField.getName(true)).append(">\";");

                    sb.append("\n\t\t\t").append("}");
                    sb.append("\n\t\t\t").append("}\n");
                } else {
                    if (xmlField.getXMLType() == null) {
                        sb.append("\n\t\t").append("if(").append(formatted).append("ATT!=\"\"){");
                        sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<").append(xmlField.getName(true)).append("\";");
                        if (attAL.size() > 0) {
                            sb.append("\n\t\t\t").append("xmlTxt+=").append(formatted).append("ATT;");
                        }
                        sb.append("\n\t\t\t").append("xmlTxt+=\"/>\";");
                        sb.append("\n\t\t").append("}\n");
                    } else {
                        sb.append("\n\t\t").append("if (this.").append(formatted).append("!=null){");
                        if (!element.isANoChildElement()) {
                            sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<").append(xmlField.getName(true)).append("\";");
                            if (attAL.size() > 0) {
                                sb.append("\n\t\t\t").append("xmlTxt+=").append(formatted).append("ATT;");
                            }
                            sb.append("\n\t\t\t").append("xmlTxt+=\">\";");
                        }
                        if (xmlField.getXMLType().getLocalType().equals("string")) {
                            sb.append("\n\t\t\t").append("xmlTxt+=this.").append(formatted).append(".replace(/>/g,\"&gt;\").replace(/</g,\"&lt;\");");
                        } else {
                            sb.append("\n\t\t\t").append("xmlTxt+=this.").append(formatted).append(";");
                        }

                        if (!element.isANoChildElement()) {
                            sb.append("\n\t\t\t").append("xmlTxt+=\"</").append(xmlField.getName(true)).append(">\";");
                        }
                        sb.append("\n\t\t").append("}");
                        if (xmlField.isRequired()) {
                            sb.append("\n\t\t").append("else{");
                            sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<").append(xmlField.getName(true)).append("\";");
                            if (attAL.size() > 0) {
                                sb.append("\n\t\t\t").append("xmlTxt+=").append(formatted).append("ATT;");
                            }
                            sb.append("\n\t\t\t").append("xmlTxt+=\"/>\";");
                            sb.append("\n\t\t").append("}\n");
                        } else if (attAL.size() > 0) {
                            sb.append("\n\t\t").append("else if(").append(formatted).append("ATT!=\"\"){");
                            sb.append("\n\t\t\t").append("xmlTxt+=\"\\n<").append(xmlField.getName(true)).append("\";");
                            sb.append("\n\t\t\t").append("xmlTxt+=").append(formatted).append("ATT;");
                            sb.append("\n\t\t\t").append("xmlTxt+=\"/>\";");
                            sb.append("\n\t\t").append("}\n");
                        }
                    }
                }
            }
        }

        return iterCounter.get();
    }

    public Map<String, String> addXMLNSAttributes() {
        // ADD SCHEMA SPECIFICATION ATTRIBUTES
        final Map<String, String> map = new TreeMap<>();
        for (final String prefix : XFTMetaManager.getPrefixes()) {
            map.put("xmlns:" + prefix, XFTMetaManager.TranslatePrefixToURI(prefix));
        }
        map.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        return map;
    }

    public String getRootName(XMLWrapperElement element) {
        final String alias = element.getProperName();
        return StringUtils.isBlank(alias) ? element.getFullXMLName() : (alias.contains(":") ? alias : element.getSchemaTargetNamespacePrefix() + ":" + alias);
    }

    /**
     * Formats a field name to a method signature.*
     *
     * @param fieldName The name of the field to be formatted.*
     *
     * @return The formatted field name.
     */
    private String formatFieldName(final String fieldName) {
        return XftStringUtils.FormatStringToMethodSignature(fieldName);
    }

    public static void GenerateJSFiles(final String javaLocation, final boolean skipXDAT) throws Exception {
        final JavaScriptGenerator generator    = new JavaScriptGenerator();
        final List<String>        elementNames = GenericUtils.convertToTypedList(XFTMetaManager.GetElementNames(), String.class);

        final StringBuilder includes = new StringBuilder();
        includes.append("\n<script type=\"text/javascript\" src=\"$content.getURI(\"scripts/generated/ClassMapping.js\")\"></script>");
        includes.append("\n<script type=\"text/javascript\" src=\"$content.getURI(\"scripts/generated/SAXEventHandler.js\")\"></script>");
        includes.append("\n<script type=\"text/javascript\" src=\"$content.getURI(\"scripts/generated/xmlsax.js\")\"></script>");

        final Map<String, String> elements = new HashMap<>();

        //SECOND PASS
        for (final String elementName : elementNames) {
            GenericWrapperElement e = GenericWrapperElement.GetElement(elementName);
            if (e.getAddin().equals("")) {
                includes.append("\n<script type=\"text/javascript\" src=\"$content.getURI(\"scripts/generated/").append(e.getFormattedName()).append(".js\")\"></script>");
                elements.put(e.getSchemaTargetNamespaceURI() + ":" + e.getLocalXMLName(), e.getFormattedName());
                elements.put(e.getFullXMLName(), e.getFormattedName());
                if (!e.getProperName().equals(e.getFullXMLName())) {
                    elements.put(e.getSchemaTargetNamespaceURI() + ":" + e.getProperName(), e.getFormattedName());
                    elements.put(e.getSchemaTargetNamespacePrefix() + ":" + e.getProperName(), e.getFormattedName());
                }
                if ((!skipXDAT) || (!e.getSchemaTargetNamespacePrefix().equalsIgnoreCase("xdat"))) {
                    generator.generateJSFile(e, javaLocation);
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append("\n\nfunction ClassMapping(){");
        builder.append("\n\t").append("this.newInstance=function(name){");
        for (Map.Entry<String, String> entry : elements.entrySet()) {
            builder.append("\n\t\t").append("if(name==\"" + entry.getKey() + "\"){");
            builder.append("\n\t\t\t").append("if(window." + entry.getValue() + "==undefined)dynamicJSLoad('" + entry.getValue() + "','generated/" + entry.getValue() + ".js');");
            builder.append("\n\t\t\t").append("return new " + entry.getValue() + "();");
            builder.append("\n\t\t").append("}");
        }
        builder.append("\n\t").append("}");
        builder.append("\n}");

        final File file = Paths.get(javaLocation, "generated/ClassMapping.js").toFile();
        file.getParentFile().mkdirs();
        if (XFT.VERBOSE) {
            System.out.println("Generating File " + file.getAbsolutePath());
        }
        try (final Reader reader = new StringReader(builder.toString()); final Writer writer = new FileWriter(file)) {
            IOUtils.copy(reader, writer);
        }
    }
}
