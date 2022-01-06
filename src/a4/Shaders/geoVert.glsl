#version 430

layout (location=0) in vec3 vertPos;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec3 vertNormal;

out vec2 tc;                //Texture coordinates
out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingHalfVector;
out vec4 shadow_coord;
out vec3 vertEyeSpacePos;


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
uniform mat4 shadowMVP;
uniform int enableLighting;
uniform float explosionFactor;        //Spider Exploding Factor
uniform int boomConditional;

void main(void)
{
    //geoEFactor = explosionFactor;
    // output vertex positions, light, and normal vectors to the rasterizer for interpolation
    vec3 vertPos3 = (mv_matrix * vec4(vertPos, 1.0)).xyz;

    varyingLightDir = light.position - vertPos3;
    varyingNormal = (norm_matrix * vec4(vertNormal, 1.0)).xyz;
    tc = texCoord;
    // calculate the half vector (L+V)
    varyingHalfVector = normalize(varyingLightDir) + normalize(-vertPos3);

    vertEyeSpacePos = (mv_matrix * vec4(vertPos.xyz, 1.0)).xyz;

    shadow_coord = shadowMVP * vec4(vertPos, 1.0);

    gl_Position = mv_matrix * vec4(vertPos, 1.0);
}
