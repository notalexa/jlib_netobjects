package not.alexa.netobjects.coding.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

@RunWith(org.junit.runners.Parameterized.class)
public class YAMLReaderTest {

	public YAMLReaderTest() {
	}
	
    @Parameters
    public static List<FileSpec> testSchemata() {
    	return Arrays.asList(
    			new FileSpec("linebreaks.yaml",true,1),
    			new FileSpec("scalars.yaml",true,1),
    			new FileSpec("file0.yaml",true,1),
    			new FileSpec("file1.yaml",true,5),
    			new FileSpec("file2.yaml",true,2),
    			new FileSpec("file3.yaml",true,3),
    			new FileSpec("file4.yaml",true,11)
    			);
    }
    
    @Parameter
    public FileSpec file;
    
    @Test
    public void readTest() {
    	try(InputStream stream=YAMLReaderTest.class.getResourceAsStream(file.name)) {
    		Yaml yaml=new Yaml(Mode.Indented,YamlScript.IDENTITY,new IncludeScript());
    		ByteArrayOutputStream out=new ByteArrayOutputStream();
    		ByteArrayOutputStream out2=new ByteArrayOutputStream();
    		Yaml jsonYaml=new Yaml(Mode.ExtendedJson);
    		OutputHandler json=new Yaml(Mode.ExtendedJson).createOutput("  ","\n", out2);
    		List<YamlException> failures=new ArrayList<>();
    		Handler handler=new Handler() {
				Handler defaultHandler=new Yaml.DefaultHandler(false,(t,e)->{
					if(e==null) {
						System.out.println("--- BEGIN\n"+t+"\n--- END");
						try {
							t.asDocument(yaml).process(json);
							json.beginDocument();
							json.write(t);
							json.endDocument();
						} catch(Throwable t0) {
							t0.printStackTrace();
						}
					} else {
						e.printStackTrace();
					}
				});
				Handler outputHandler=yaml.createOutput("  ", "\n", out);

				@Override
				public void beginArray(boolean key,List<Token> modifier) throws YamlException {
					System.out.println("Begin (key="+key+", array=true, modifier="+modifier+")");
					defaultHandler.beginArray(key, modifier);
					outputHandler.beginArray(key, modifier);
				}

				@Override
				public void beginObject(boolean key,List<Token> modifier) throws YamlException {
					System.out.println("Begin (key="+key+", array=false, modifier="+modifier+")");
					defaultHandler.beginObject(key, modifier);
					outputHandler.beginObject(key, modifier);
				}

				@Override
				public void scalar(boolean key,List<Token> modifier,Token token) throws YamlException {
					System.out.println("Token "+token+" with modifiers="+modifier+", key="+key);
					defaultHandler.scalar(key,modifier,token);
					outputHandler.scalar(key, modifier, token);
				}
				
				@Override
				public void endObject(boolean key) throws YamlException {
					System.out.println("  End (key="+key+", array=false)");					
					defaultHandler.endObject(key);
					outputHandler.endObject(key);
				}

				@Override
				public void endArray(boolean key) throws YamlException {
					System.out.println("  End (key="+key+", array=true)");					
					defaultHandler.endArray(key);
					outputHandler.endArray(key);
				}

				@Override
				public void beginDocument() throws YamlException {
					System.out.println("Init");
					defaultHandler.beginDocument();
					outputHandler.beginDocument();
				}

				@Override
				public void endDocument() throws YamlException {
					System.out.println("Finished");
					defaultHandler.endDocument();
					outputHandler.endDocument();
				}

				@Override
				public void onError(YamlException e) throws YamlException {
					failures.add(e);
					try {
						defaultHandler.onError(e);
						outputHandler.onError(e);
					} catch(Throwable t) {
						
					}
				}
				
			};
    		assert(yaml.parse(stream, handler)==handler);
			System.out.write(out.toByteArray());
			System.out.write(out2.toByteArray());
			for(Exception ex:failures) {
				ex.printStackTrace();
			}
			assertEquals(0,failures.size());
    		assert(file.valid);
    	} catch(IOException t) {
    		t.printStackTrace();
    		assert(!file.valid);
    	}
    	
    }

    @Test
    public void skipTest() {
    	System.out.println("\n"+file.name);
    	try(InputStream stream=YAMLReaderTest.class.getResourceAsStream(file.name)) {
    		Yaml yaml=new Yaml(Mode.Indented,YamlScript.IDENTITY,new IncludeScript());
    		int count=0;
    		for(Document doc:yaml.parse(stream)) {
    			System.out.println("Skip Document #"+(++count));
    		}
    		assertEquals(file.documents, count);
    	} catch(Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    
    static class FileSpec {
    	String name;
    	boolean valid;
    	int documents;
    	FileSpec(String name,boolean valid,int documents) {
    		this.name=name;
    		this.valid=valid;
    		this.documents=documents;
    	}
    }
    
    public static void main(String[] args) {
    	System.out.println("\u2029");
    }
}
