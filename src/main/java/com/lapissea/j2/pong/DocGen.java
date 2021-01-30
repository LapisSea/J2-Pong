package com.lapissea.j2.pong;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lapissea.util.TextUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.List;
import java.util.function.Consumer;

public class DocGen{
	
	public static void main(String[] args) throws IOException{
		var root=new File("C:\\Programming\\Java\\Java 2\\Pong\\src\\main\\java");
		
		JsonObject rootJson=new JsonObject();
		
		process(root, file->{
			if(!file.getPath().endsWith(".java")) return;
			var classPath=file.getPath()
			                  .substring(root.getPath().length()+1, file.getPath().length()-5)
			                  .replace("\\", ".")
			                  .replace("/", ".");
			
			processClass(classPath, rootJson);
		});
		try(FileWriter log=new FileWriter("codeDoc.json")){
			log.write(new GsonBuilder().setPrettyPrinting().create().toJson(rootJson));
		}
	}
	private static void process(File file, Consumer<File> processor){
		processor.accept(file);
		if(file.isDirectory()){
			var fs=file.listFiles();
			if(fs!=null){
				for(File f : fs){
					process(f, processor);
				}
			}
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private static void processClass(String classPath, JsonObject dest){
		try{
			Class<?> c=Class.forName(classPath);
			if(!Modifier.isPublic(c.getModifiers())) return;
			
			if(!dest.has("classes")) dest.add("classes", new JsonArray());
			JsonArray classes=dest.getAsJsonArray("classes");
			
			JsonObject classInfo=new JsonObject();
			classes.add(classInfo);
			var data=TextUtil.mapObjectValues(c);
			
			for(String name : List.of("name", "superclass", "genericSuperclass", "interface", "enum", "record", "typeParameters",
			                          "interfaces", "genericInterfaces", "enclosingClass", "enumConstants", "annotations")){
				var val=data.get(name);
				if(val==null) continue;
				if(val instanceof List<?> l&&l.isEmpty()) continue;
				if(val instanceof Object[] a&&a.length==0) continue;
				
				classInfo.addProperty(name, TextUtil.toString(val));
			}
			
			{
				JsonArray object=new JsonArray();
				classInfo.add("fields", object);
				for(Field f : (Field[])data.get("declaredFields")){
					if(!Modifier.isPublic(f.getModifiers())) continue;
					
					JsonObject fo=new JsonObject();
					object.add(fo);
					fo.addProperty("name", f.getName());
					fo.addProperty("type", f.getGenericType().toString());
				}
			}
			{
				JsonArray object=new JsonArray();
				classInfo.add("methods", object);
				for(Method f : (Method[])data.get("declaredMethods")){
					if(!Modifier.isPublic(f.getModifiers())) continue;
					
					JsonObject fo=new JsonObject();
					object.add(fo);
					fo.addProperty("name", f.getName());
					fo.addProperty("returns", f.getGenericReturnType().toString());
					if(f.getGenericParameterTypes().length!=0){
						JsonArray args=new JsonArray();
						for(Type type : f.getGenericParameterTypes()){
							args.add(type.toString());
						}
						fo.add("arguments", args);
					}
					fo.addProperty("static", Modifier.isStatic(f.getModifiers()));
					if(f.getGenericExceptionTypes().length!=0){
						JsonArray thrs=new JsonArray();
						for(Type type : f.getGenericExceptionTypes()){
							thrs.add(type.toString());
						}
						fo.add("throws", thrs);
					}
					fo.addProperty("varargs", f.isVarArgs());
					
				}
			}
			{
				JsonArray object=new JsonArray();
				classInfo.add("constructors", object);
				for(Constructor<?> cons : (Constructor[])data.get("constructors")){
					if(!Modifier.isPublic(cons.getModifiers())) continue;
					
					JsonObject fo=new JsonObject();
					object.add(fo);
					fo.addProperty("name", cons.getName());
					if(cons.getGenericParameterTypes().length!=0){
						JsonArray args=new JsonArray();
						for(Type type : cons.getGenericParameterTypes()){
							args.add(type.toString());
						}
						fo.add("arguments", args);
					}
				}
			}
			{
				JsonArray object=new JsonArray();
				for(Class<?> cls : (Class[])data.get("classes")){
					if(!Modifier.isPublic(cls.getModifiers())) continue;
					
					JsonObject fo=new JsonObject();
					object.add(fo);
					processClass(cls.getName(), fo);
				}
				if(object.size()>0) classInfo.add("classes", object);
			}
			
		}catch(ReflectiveOperationException e){
			e.printStackTrace();
		}
		
	}
}
