package com.lapissea.j2.pong.game.server;

import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static javax.xml.transform.OutputKeys.*;

public class Persistence{
	
	private static final String SAVE_FILENAME    =Utils.getConfig().getString("saveFile");
	private static final String DTD_SAVE_FILENAME=SAVE_FILENAME+".dtd";
	private static final String XML_SAVE_FILENAME=SAVE_FILENAME+".xml";
	
	private static Document createDocument(String element) throws ParserConfigurationException{
		DocumentBuilderFactory factory          =DocumentBuilderFactory.newInstance();
		DocumentBuilder        builder          =factory.newDocumentBuilder();
		DOMImplementation      domImplementation=builder.getDOMImplementation();
		DocumentType           documentType     =domImplementation.createDocumentType("DOCTYPE", null, DTD_SAVE_FILENAME);
		return domImplementation.createDocument(null, element, documentType);
	}
	
	private static Element createAppendElement(Document document, String tagName, Object data){
		return createAppendElement(document.getDocumentElement(), tagName, data);
	}
	
	private static Element createAppendElement(Element parent, String tagName, Object data){
		Document doc    =parent.getOwnerDocument();
		Element  element=doc.createElement(tagName);
		if(data!=null){
			element.appendChild(doc.createTextNode(data.toString()));
		}
		parent.appendChild(element);
		return element;
	}
	
	private static void saveDoc(Document document, String fileName) throws TransformerException{
		Transformer transformer=TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(INDENT, "yes");
		transformer.setOutputProperty(DOCTYPE_SYSTEM, document.getDoctype().getSystemId());
		transformer.transform(new DOMSource(document), new StreamResult(new File(fileName)));
	}
	
	
	public static void saveGameState(GameState source, Map<Long, Profile> profiles){
		try{
			ensureDef();
			
			Document document=createDocument("state");
			Element  values  =createAppendElement(document, "values", null);
			
			createAppendElement(values, "playerSize", source.playerSize.get());
			createAppendElement(values, "ballSpeed", source.ballSpeed.get());
			createAppendElement(values, "playerSpeed", source.playerSpeed.get());
			
			if(!profiles.isEmpty()){
				Element profilesEl=document.createElement("profiles");
				document.getDocumentElement().appendChild(profilesEl);
				
				for(Profile value : profiles.values()){
					Element profile=createAppendElement(profilesEl, "profile", null);
					createAppendElement(profile, "id", value.id());
					createAppendElement(profile, "name", value.userName());
				}
			}else return;
			
			saveDoc(document, XML_SAVE_FILENAME);
			
		}catch(ParserConfigurationException|TransformerException|IOException e){
			e.printStackTrace();
		}
	}
	private static void ensureDef() throws IOException{
		
		File dtd=new File(DTD_SAVE_FILENAME);
//	    	if(dtd.exists()&&dtd.isFile()) return;
		
		try(var data=Utils.makeRawUrl("/"+DTD_SAVE_FILENAME).openStream();
		    var out=new FileOutputStream(dtd)){
			data.transferTo(out);
		}
	}
	
	public static void loadGameState(GameState target, Consumer<Profile> acceptProfile){
		try{
			ensureDef();
			
			
			enum ContextTag{
				
				ID("id"),
				NAME("name"),
				PLAYER_SIZE("playerSize"),
				BALL_SPEED("ballSpeed"),
				PLAYER_SPEED("playerSpeed"),
				;
				
				private final String name;
				
				ContextTag(String name){
					this.name=name;
				}
				
				private static ContextTag byName(String name){
					for(ContextTag value : values()){
						if(value.name.equals(name)){
							return value;
						}
					}
					return null;
				}
			}
			
			class ProfilesHandler extends DefaultHandler{
				
				private final Consumer<Profile> acceptProfile;
				private final GameState         state;
				
				private ProfilesHandler(GameState state, Consumer<Profile> acceptProfile){
					this.acceptProfile=acceptProfile;
					this.state=state;
				}
				
				private ContextTag tag;
				
				private Long   id;
				private String name;
				
				@Override
				public void startDocument() throws SAXException{
					tag=null;
				}
				
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes){
					tag=ContextTag.byName(qName);
				}
				
				@Override
				public void characters(char[] ch, int start, int length){
					String value=new String(ch, start, length);
					if(tag==null) return;
					
					switch(tag){
					case ID -> {
						id=Long.parseLong(value);
						flushProfile();
					}
					case NAME -> {
						name=value;
						flushProfile();
					}
					case BALL_SPEED -> state.ballSpeed.set(Float.parseFloat(value));
					case PLAYER_SIZE -> state.playerSize.set(Float.parseFloat(value));
					case PLAYER_SPEED -> state.playerSpeed.set(Float.parseFloat(value));
					}
				}
				
				private void flushProfile(){
					if(id==null||name==null) return;
					acceptProfile.accept(new Profile(name, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), id));
					id=null;
					name=null;
				}
				
				
				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException{
					tag=null;
				}
			}
			
			SAXParserFactory factory=SAXParserFactory.newInstance();
			factory.setValidating(true);
			factory.newSAXParser().parse(new File(XML_SAVE_FILENAME), new ProfilesHandler(target, acceptProfile));
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
}
