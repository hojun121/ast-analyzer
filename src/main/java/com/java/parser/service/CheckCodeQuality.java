package com.java.parser.service;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.visitor.VoidVisitorAdapter;



@RestController
public class CheckCodeQuality {	


	
	@GetMapping("read")
	public String createCompilationUnit() {		
		
	try {
// 		System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		File file = new File("/home/AbstractSyntaxTree/SampleTest/src/com/sample/SampleApp.java");
		CompilationUnit cu = JavaParser.parse(new FileInputStream(file));
		System.out.println("compilation unit " + cu);
		System.out.println("___________________________________________________________________");
		System.out.println("cu.getPackage() " + cu.getPackage().getName());
		/*System.out.println("cu.getTypes() + package " + cu.getTypes());		
		System.out.println("cu.getParentNode() " + cu.getParentNode());
		System.out.println("cu.getData() " + cu.getData());
		System.out.println("cu.getClass() " + cu.getClass());
		System.out.println("cu.getBeginColumn() " + cu.getBeginColumn());
		System.out.println("cu.getOrphanComments() " + cu.getOrphanComments());
		System.out.println("cu.getComment() " + cu.getComment());
		System.out.println("cu.getEndLine() " + cu.getEndLine());
		System.out.println("cu.getBeginLine() " + cu.getBeginLine());*/
		
		 //Scan the project and upload the results in Sonar Server
		runSonarScanner();
		
		//Read and print Sonar report
	    List<List> listContainer = printSonarReport();
	    for(List<String> container : listContainer) {	    	
	    	System.out.println("squid values : " + container.get(0));
	    	
	    }

		List<Class> classes = getClassesOfPackage("com.java.parser.visitor");
		
		//Get Handler
		getHandler(listContainer, classes, cu);

		/*for(Class c : classes) {
			VoidVisitorAdapter o;
			if(c.getSimpleName().startsWith("Constructor")){
				System.out.println("test");
			}
			try {
				o = (VoidVisitorAdapter) c.newInstance();
				cu.accept(o, cu);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}*/
		
		FileOutputStream fos = new FileOutputStream(file);
		byte[] strToBytes = cu.toString().getBytes();
		fos.write(strToBytes);
	    fos.close();
	   
	    runSonarScanner();
	    List<List> listContainer1 = printSonarReport();
	    for(List<String> container1 : listContainer1) {	    	
	    	System.out.println("squid values : " + container1.get(0));
	    	
	    }
	   
	    return cu.toString();
		
	} catch (ParseException  | IOException pfi ) {
		pfi.printStackTrace();
		return "Fail";
	} 
}
	
	
	private void getHandler(List<List> listContainer, List<Class> handlers, CompilationUnit cu) {
		
		String portion = null;
		
		for(List<String> container : listContainer) {	    	
	    	System.out.println("squid values : " + container.get(0));
	    	if(container.get(0).contains(":S")) {
	    		 int index1 = container.get(0).indexOf('S');
	    		 portion = container.get(0).substring(index1);
	    	}
	    	for(Class c : handlers) {
				VoidVisitorAdapter o;
				
				int index = c.getSimpleName().indexOf('_');
				String rule = c.getSimpleName().substring(0,index);
				
				if(portion.equals(rule)) {
				try {
					o = (VoidVisitorAdapter) c.newInstance();
					cu.accept(o, cu);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			}
	    }
	}

	private static List<Class> getClassesOfPackage(String pkgname){
		List<Class> classes = new ArrayList<Class>();

	    File directory = null;
	    String fullPath;
	    
	    //String pkgname = "com.java.parser.visitor";
	    String relPath = pkgname.replace('.', '/');
	   // System.out.println("ClassDiscovery: Package: " + pkgname + " becomes Path:" + relPath);
	    
	    URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
	    //System.out.println("ClassDiscovery: Resource = " + resource);
	    if (resource == null) {
	        throw new RuntimeException("No resource for " + relPath);
	    }
	    
	    fullPath = resource.getFile();
	   // System.out.println("ClassDiscovery: FullPath = " + resource);
	    
	    try {
	        directory = new File(resource.toURI());
	    } catch (URISyntaxException e) {
	        throw new RuntimeException(pkgname, e);
	    } catch (IllegalArgumentException e) {
	        directory = null;
	    }
	    System.out.println("ClassDiscovery: Directory = " + directory);
	    
	    if (directory != null && directory.exists()) {

	        // Get the list of the files contained in the package
	        String[] files = directory.list();
	        for (int i = 0; i < files.length; i++) {

	            // we are only interested in .class files
	            if (files[i].endsWith(".class")) {

	                // removes the .class extension
	                String className = pkgname + '.' + files[i].substring(0, files[i].length() - 6);

	                System.out.println("ClassDiscovery: className = " + className);

	                try {
	                    classes.add(Class.forName(className));
	                } catch (ClassNotFoundException e) {
	                    throw new RuntimeException("ClassNotFoundException loading " + className);
	                }
	            }
	        }
	    }
	    return classes;
		
	}


	private void runSonarScanner() {
		try {
			Runtime.getRuntime().exec("cmd /c sonar-scanner.bat", null, new File("/workspace4/SampleTest"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
