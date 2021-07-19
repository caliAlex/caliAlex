#version 430

layout (triangles) in;

//Inputs from vertex shader
in vec3 varyingNormal[];
in vec3 varyingLightDir[];
in vec3 varyingHalfVector[];
in vec2 tc[];               //Bring in the textrue coordinates from geoVertShader


//Outputs through rasterizer to the frag shader
out vec3 varyingNormalG;
out vec3 varyingLightDirG;
out vec3 varyingHalfVectorG;
out vec2 tcG;   //Texture Coordinates

layout (triangle_strip, max_vertices=3) out;
//layout (points, max_vertices=1) out;

struct PositionalLight
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};
struct Material
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform int enableLighting;
uniform float explosionFactor;
uniform int boomConditional;

void main (void)
{
    vec4 triangleNormal = vec4(((varyingNormal[0]+varyingNormal[1]+varyingNormal[2])/3.0),1.0);
    vec4 updatedTriangleNormals;
    mat4 tempMatrix;
    gl_PointSize = 5;


        for (int i=0; i<3; i++)
        {
            gl_Position = proj_matrix * (gl_in[i].gl_Position + (normalize(triangleNormal)));

            if (boomConditional == 1){
                    gl_Position = proj_matrix * (gl_in[i].gl_Position + (normalize(triangleNormal) * explosionFactor*10));

                    tempMatrix = proj_matrix;
                    updatedTriangleNormals = triangleNormal;
                }
            else {  //Reconstruct the spider
                gl_Position = proj_matrix * (gl_in[i].gl_Position - (-normalize(triangleNormal) * explosionFactor*10));
                tempMatrix = proj_matrix;
                updatedTriangleNormals = triangleNormal;
            }

            varyingNormalG = varyingNormal[i];
            varyingLightDirG = varyingLightDir[i];
            varyingHalfVectorG = varyingHalfVector[i];
            tcG = tc[i];

            //Specifies vertex to be output
            EmitVertex();
        }

    //for (int i=0; i<3; i++)
      //  gl_Position = proj_matrix * (gl_in[i].gl_Position + (normalize(triangleNormal)));

    //Indicates completion of buildint a particular primitive
    EndPrimitive();

}
