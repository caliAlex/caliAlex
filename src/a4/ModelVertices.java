package a4;

import org.joml.Vector2f;
import org.joml.Vector3f;


public class ModelVertices{
    //LightModel(int numModelVertices, Vector3f[] modelVertices, Vector2f[] modelTexCoords, Vector2f[] modelNorms, float[] pValPar, float[] texPar, float[] normPar){

    private int numObjVertices;
    private Vector3f[] vertices, normals;
    private Vector2f[] texCoords;
    private float[] pvalues, tvalues, nvalues;

    ModelVertices(ImportedModel modelPar) {
        numObjVertices = modelPar.getNumVertices();
        vertices = modelPar.getVertices();
        texCoords = modelPar.getTexCoords();
        normals = modelPar.getNormals();

        pvalues = new float[numObjVertices * 3];      // vertex positions
        tvalues = new float[numObjVertices * 2];      // texture coords
        nvalues = new float[numObjVertices * 3];      // normal vectors

        for (int i = 0; i < numObjVertices; i++) {
            pvalues[i * 3] = (float) (vertices[i]).x();
            pvalues[i * 3 + 1] = (float) (vertices[i]).y();
            pvalues[i * 3 + 2] = (float) (vertices[i]).z();

            tvalues[i * 2] = (float) (texCoords[i]).x();
            tvalues[i * 2 + 1] = (float) (texCoords[i]).y();

            nvalues[i * 3] = (float) (normals[i]).x();
            nvalues[i * 3 + 1] = (float) (normals[i]).y();
            nvalues[i * 3 + 2] = (float) (normals[i]).z();
        }//EndFor
    }//End Constructor

    public float[] getPValues(){
        return pvalues;
    }

    public float[] getTValues(){
        return tvalues;
    }

    public float[] getNValues(){
        return nvalues;
    }
}//End Class
