#version 430

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingNormal;
out vec3 varyingHalfVector;
out vec2 tc;
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

layout (binding=0) uniform samplerCube tex_map;

void main(void){
    //Output vertex position rasterizer for interpolation
    varyingVertPos = (mv_matrix * vec4(position, 1.0)).xyz;

    //Get vector from vertex to light and output to rasterizer for interpolation
    varyingLightDir = light.position - varyingVertPos;
    //get vertex normal vector in eye space & output to rasterizer for interp.
    varyingNormal = (norm_matrix * vec4(normal, 1.0)).xyz;

    //Vert eye-space no h-map
    vertEyeSpacePos = (mv_matrix * vec4(vertPos.xyz, 1.0)).xyz;

    //Calculate 1/2 vector (L+V)
    //varyingHalfVector =
    //	normalize(normalize(varyingLightDir)
    //	+ normalize(-varyingVertPos)).xyz;
    varyingHalfVector = varyingLightDir-varyingVertPos.xyz;

    //Calculate shadow coordinates for 2nd pass
    shadow_coord = shadowMVP * vec4(vertPos, 1.0);

    gl_Position = proj_matrix * mv_matrix * vec4(position, 1.0);
}