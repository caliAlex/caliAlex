package a4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.joml.*;

public class ImportedModel
{
    private Vector3f[] vertices;
    private Vector2f[] texCoords;
    private Vector3f[] normals;
    private int numVertices;

    public ImportedModel(String filename)
    {	ModelImporter modelImporter = new ModelImporter();
        try
        {	modelImporter.parseOBJ(filename);
            numVertices   = modelImporter.getNumVertices();
            float[] verts = modelImporter.getVertices();
            float[] tcs   = modelImporter.getTextureCoordinates();
            float[] norm  = modelImporter.getNormals();

            vertices = new Vector3f[numVertices];
            texCoords = new Vector2f[numVertices];
            normals = new Vector3f[numVertices];

            for(int i=0; i<vertices.length; i++)
            {	vertices[i] = new Vector3f();
                vertices[i].set(verts[i*3], verts[i*3+1], verts[i*3+2]);
                texCoords[i] = new Vector2f();
                texCoords[i].set(tcs[i*2], tcs[i*2+1]);
                normals[i] = new Vector3f();
                normals[i].set(norm[i*3], norm[i*3+1], norm[i*3+2]);
            }
        } catch (IOException e)
        { e.printStackTrace();
        }	}

    public int getNumVertices() { return numVertices; }
    public Vector3f[] getVertices() { return vertices; }
    public Vector2f[] getTexCoords() { return texCoords; }
    public Vector3f[] getNormals() { return normals; }

    private class ModelImporter
    {	// values as read from OBJ file
        private ArrayList<Float> vertVals = new ArrayList<Float>();
        private ArrayList<Float> triangleVerts = new ArrayList<Float>();
        private ArrayList<Float> textureCoords = new ArrayList<Float>();

        // values stored for later use as vertex attributes
        private ArrayList<Float> stVals = new ArrayList<Float>();
        private ArrayList<Float> normals = new ArrayList<Float>();
        private ArrayList<Float> normVals = new ArrayList<Float>();

        public void parseOBJ(String filename) throws IOException
        {	InputStream input = ModelImporter.class.getResourceAsStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = br.readLine()) != null)
            {	if(line.startsWith("v "))			// vertex position ("v" case)
            {	for(String s : (line.substring(2)).split(" "))
            {	vertVals.add(Float.valueOf(s));
            }	}
            else if(line.startsWith("vt"))			// texture coordinates ("vt" case)
            {	for(String s : (line.substring(3)).split(" "))
            {	stVals.add(Float.valueOf(s));
            }	}
            else if(line.startsWith("vn"))			// vertex normals ("vn" case)
            {	for(String s : (line.substring(3)).split(" "))
            {	normVals.add(Float.valueOf(s));
            }	}
            else if(line.startsWith("f"))			// triangle faces ("f" case)
            {	for(String s : (line.substring(2)).split(" "))
            {	String v = s.split("/")[0];
                String vt = s.split("/")[1];
                String vn = s.split("/")[2];

                int vertRef = (Integer.valueOf(v)-1)*3;
                int tcRef   = (Integer.valueOf(vt)-1)*2;
                int normRef = (Integer.valueOf(vn)-1)*3;

                triangleVerts.add(vertVals.get(vertRef));
                triangleVerts.add(vertVals.get((vertRef)+1));
                triangleVerts.add(vertVals.get((vertRef)+2));

                textureCoords.add(stVals.get(tcRef));
                textureCoords.add(stVals.get(tcRef+1));

                normals.add(normVals.get(normRef));
                normals.add(normVals.get(normRef+1));
                normals.add(normVals.get(normRef+2));
            }	}	}
            input.close();
        }

        public int getNumVertices() { return (triangleVerts.size()/3); }

        public float[] getVertices()
        {	float[] p = new float[triangleVerts.size()];
            for(int i = 0; i < triangleVerts.size(); i++)
            {	p[i] = triangleVerts.get(i);
            }
            return p;
        }

        public float[] getTextureCoordinates()
        {	float[] t = new float[(textureCoords.size())];
            for(int i = 0; i < textureCoords.size(); i++)
            {	t[i] = textureCoords.get(i);
            }
            return t;
        }

        public float[] getNormals()
        {	float[] n = new float[(normals.size())];
            for(int i = 0; i < normals.size(); i++)
            {	n[i] = normals.get(i);
            }
            return n;
        }
    }
}
