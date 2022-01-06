package a4;

public class SkyBox {
    public static float[] getVertices() {
        float[] cubeVertexPositions =
                {-1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
                        -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f
                };
        return cubeVertexPositions;
    }

    public static float[] getTextures() {
        float[] cubeTextureCoord =
                {1.00f, 0.6666666f, 1.00f, 0.3333333f, 0.75f, 0.3333333f,       // back face lower right
                        0.75f, 0.3333333f, 0.75f, 0.6666666f, 1.00f, 0.6666666f,    // back face upper left
                        0.75f, 0.3333333f, 0.50f, 0.3333333f, 0.75f, 0.6666666f,    // right face lower right
                        0.50f, 0.3333333f, 0.50f, 0.6666666f, 0.75f, 0.6666666f,    // right face upper left
                        0.50f, 0.3333333f, 0.25f, 0.3333333f, 0.50f, 0.6666666f,    // front face lower right
                        0.25f, 0.3333333f, 0.25f, 0.6666666f, 0.50f, 0.6666666f,    // front face upper left
                        0.25f, 0.3333333f, 0.00f, 0.3333333f, 0.25f, 0.6666666f,    // left face lower right
                        0.00f, 0.3333333f, 0.00f, 0.6666666f, 0.25f, 0.6666666f,    // left face upper left
                        0.25f, 0.3333333f, 0.50f, 0.3333333f, 0.50f, 0.0000000f,    // bottom face upper right
                        0.50f, 0.0000000f, 0.25f, 0.0000000f, 0.25f, 0.3333333f,    // bottom face lower left
                        0.25f, 1.0000000f, 0.50f, 1.0000000f, 0.50f, 0.6666666f,    // top face upper right
                        0.50f, 0.6666666f, 0.25f, 0.6666666f, 0.25f, 1.0000000f     // top face lower left
                };
        return cubeTextureCoord;
    }
}

